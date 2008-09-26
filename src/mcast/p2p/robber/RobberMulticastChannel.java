package mcast.p2p.robber;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mcast.p2p.AbstractMulticastChannel;
import mcast.p2p.Collective;
import mcast.p2p.LocationPool;
import mcast.p2p.Pool;
import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.admin.PieceIndexSetFactory;
import mcast.p2p.net.IndividualConnectionNegotiator;
import mcast.p2p.net.P2PConnectionFactory;
import mcast.p2p.net.P2PConnectionNegotiator;
import mcast.p2p.net.P2PConnectionPool;
import mcast.p2p.storage.Storage;
import mcast.p2p.util.Convert;

import org.apache.log4j.Logger;

public class RobberMulticastChannel extends AbstractMulticastChannel 
        implements Config
{

    private Logger logger = Logger.getLogger(RobberMulticastChannel.class);

    private final Pool pool;
    private final P2PConnectionPool<RobberConnection> localConnectionPool;
    private final P2PConnectionPool<RobberConnection> globalConnectionPool;
    private volatile RobberAdmin admin;

    public RobberMulticastChannel(Ibis ibis, IbisIdentifier[] members,
            String name) throws IOException {
        this(ibis, new LocationPool(name, members));
    }
    
    public RobberMulticastChannel(Ibis ibis, Pool pool) throws IOException { 
        super(ibis.identifier());

        logger.info("creating Robber multicast channel " + pool.getName());

        this.pool = pool;
        this.admin = null;

        P2PConnectionFactory<RobberConnection> connectionFactory = 
            new RobberConnectionFactory(pool.getName());

        Collective myCollective = pool.getCollective(ibis.identifier());
        List<IbisIdentifier> myMembers = myCollective.getMembers();    
        String myCollectiveName = myCollective.getName();
        
        if (myMembers.size() > 1) {
            String localPoolName = myCollectiveName + '@' + pool.getName(); 
            
            logger.info("connecting to local peers in " + localPoolName);
            
            P2PConnectionNegotiator<RobberConnection> localNegotiator = 
                new IndividualConnectionNegotiator<RobberConnection>(
                    localPoolName, myMembers, ibis, connectionFactory, 
                    LOCAL_MIN_PEERS);

            localConnectionPool = 
                new P2PConnectionPool<RobberConnection>(localNegotiator);
        } else {
            logger.info("no local connections needed in collective " + 
                    myCollectiveName);
            localConnectionPool = null;
        }

        List<IbisIdentifier> participants = pool.getEverybody();
        
        if (myMembers.size() < participants.size()) {
            logger.info("connecting to global peers");

            PossiblePeersGenerator<IbisIdentifier> globalPeersGenerator = 
                new GlobalPeersGenerator(pool);

            List<IbisIdentifier> globalNodes = 
                globalPeersGenerator.generatePossiblePeers(me);
            
            P2PConnectionNegotiator<RobberConnection> globalNegotiator = 
                new IndividualConnectionNegotiator<RobberConnection>(
                        pool.getName(), globalNodes, ibis, connectionFactory, 
                        GLOBAL_MIN_PEERS);

            globalConnectionPool = 
                new P2PConnectionPool<RobberConnection>(globalNegotiator); 
        } else {
            logger.info("no global connections needed");
            globalConnectionPool = null;
        }

        // print config settings
        logger.info("- local min. peers:      " + LOCAL_MIN_PEERS);
        logger.info("- global min. peers:     " + GLOBAL_MIN_PEERS);
        logger.info("- max. pending requests: " + 
                mcast.p2p.bittorrent.Config.MAX_PENDING_REQUESTS);
    }

    protected void doMulticastStorage(Storage storage, 
            Set<IbisIdentifier> roots, PieceIndexSet possession) 
            throws IOException {

        PieceIndexSet work = initWork(storage.getPieceCount(), possession);
        logger.info("my work is " + work.size() + " pieces: " + work);

        Collective myCollective = pool.getCollective(me);
        List<IbisIdentifier> myMembers = myCollective.getMembers();
        boolean doStealing = Collections.disjoint(myMembers, roots);

        logger.info("I do stealing: " + doStealing);

        // use a set of members instead of a list, since set.contains() is
        // faster than list.contains()
        Set<IbisIdentifier> myMemberSet = new HashSet<IbisIdentifier>(myMembers);
        admin = new RobberAdminImpl(storage.getPieceCount(), possession,
                myMemberSet, work, doStealing);

        logger.debug("initializing connections");
        if (localConnectionPool != null) {
            localConnectionPool.init(storage, admin);
        }
        if (globalConnectionPool != null) {
            globalConnectionPool.init(storage, admin);
        }

        logger.debug("starting connections");
        if (localConnectionPool != null) {
            localConnectionPool.start();
        }
        if (globalConnectionPool != null) {
            globalConnectionPool.start();
        }

        admin.waitUntilAllPiecesReceived();
    }

    protected void doFlush() throws IOException {
        if (globalConnectionPool != null) {
            globalConnectionPool.stop();
        }
        if (localConnectionPool != null) {
            localConnectionPool.stop();
        }
    }

    private PieceIndexSet initWork(int totalPieces, PieceIndexSet possession) {
        // each node starts with an equal share of work

        Collective myCollective = pool.getCollective(me);
        List<IbisIdentifier> myMembers = myCollective.getMembers();
        int myRankInCluster = myMembers.indexOf(me);
        
        double share = totalPieces / (double) (myMembers.size());

        int firstPieceIndex = (int) Math.floor(myRankInCluster * share);
        int lastPieceIndex = (int) (Math.floor((myRankInCluster + 1) * share) - 1);

        PieceIndexSet result = PieceIndexSetFactory.createEmptyPieceIndexSet();
        result.init(firstPieceIndex, lastPieceIndex - firstPieceIndex + 1);

        return result;
    }

    public void printStats() throws IOException {
        if (admin != null) {
            if (localConnectionPool != null) {
                localConnectionPool.printStats();
            }

            if (globalConnectionPool != null) {
                globalConnectionPool.printStats();
            }

            printPieceOriginStats();

            admin.printStats();
        }
    }

    private void printPieceOriginStats() {
        int piecesReceivedLocal = getTotalPiecesReceived(localConnectionPool);
        int piecesReceivedGlobal = getTotalPiecesReceived(globalConnectionPool);

        double totalPieces = admin.getNoTotalPieces();

        String percReceivedLocal = 
            Convert.round((double) piecesReceivedLocal / totalPieces * 100, 2);
        String percReceivedGlobal = 
            Convert.round((double) piecesReceivedGlobal / totalPieces * 100, 2);

        Config.statsLogger.info(me + " pool_stats rcvd " + "local " + 
                piecesReceivedLocal + " = " + percReceivedLocal + "% " + 
                "global " + piecesReceivedGlobal + " = " + 
                percReceivedGlobal + "%");
    }

    private int getTotalPiecesReceived(Iterable<RobberConnection> connections) {
        int result = 0;

        if (connections != null) {
            synchronized (connections) {
                for (RobberConnection connection : connections) {
                    result += connection.getPiecesReceived();
                }
            }
        }

        return result;
    }

    protected void doClose() throws IOException {
        if (globalConnectionPool != null) {
            globalConnectionPool.close();
        }
        if (localConnectionPool != null) {
            localConnectionPool.close();
        }
    }

}
