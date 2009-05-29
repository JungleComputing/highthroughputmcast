package mcast.ht.robber;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import mcast.ht.AbstractMulticastChannel;
import mcast.ht.Collective;
import mcast.ht.LocationPool;
import mcast.ht.Pool;
import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.bittorrent.BitTorrentConnection;
import mcast.ht.graph.AllOtherPeersGenerator;
import mcast.ht.graph.DirectedGraph;
import mcast.ht.graph.DirectedGraphFactory;
import mcast.ht.graph.PossiblePeersGenerator;
import mcast.ht.net.Doorbell;
import mcast.ht.net.GraphConnectionNegotiator;
import mcast.ht.net.IndividualConnectionNegotiator;
import mcast.ht.net.P2PConnectionFactory;
import mcast.ht.net.P2PConnectionNegotiator;
import mcast.ht.net.P2PConnectionPool;
import mcast.ht.storage.Storage;
import mcast.ht.util.Convert;

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
            
            P2PConnectionNegotiator<RobberConnection> n = null;
            
            if (TEST_CONNECTED) {
                // create a deterministic random graph and test it for 
                // connectedness
                PossiblePeersGenerator<IbisIdentifier> peerGenerator = 
                        new AllOtherPeersGenerator<IbisIdentifier>(myMembers);

                long seed = generateSeed(myMembers);

                DirectedGraph<IbisIdentifier> g = null;
                do {
                    logger.debug("creating random local communication graph");
                    g = DirectedGraphFactory.createMinDegreeRandomGraph(
                            myMembers, LOCAL_MIN_PEERS, seed, peerGenerator);
                    seed += 1;
                } while (!g.isWeaklyConnected());
                
                n = new GraphConnectionNegotiator<RobberConnection>(g, ibis, 
                        connectionFactory);
            } else {
                n = new IndividualConnectionNegotiator<RobberConnection>(
                        localPoolName, myMembers, ibis, connectionFactory, 
                        LOCAL_MIN_PEERS);
            }
            
            localConnectionPool = new P2PConnectionPool<RobberConnection>(n);
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
            
            P2PConnectionNegotiator<RobberConnection> n = null;
            
            if (TEST_CONNECTED) {
                // create a deterministic random global graph and test it for 
                // connectedness
                long seed = generateSeed(participants);

                DirectedGraph<IbisIdentifier> g = null;
                DirectedGraph<Collective> c = null;
                
                do {
                    g = DirectedGraphFactory.createMinDegreeRandomGraph(
                            participants, GLOBAL_MIN_PEERS, seed, 
                            globalPeersGenerator);
                    seed += 1;
                    c = createCollectiveGraph(g, pool);
                } while (!c.isWeaklyConnected()); 
                
                n = new GraphConnectionNegotiator<RobberConnection>(g, ibis, 
                        connectionFactory);
            } else {
                n = new IndividualConnectionNegotiator<RobberConnection>(
                        pool.getName(), globalNodes, ibis, connectionFactory, 
                        GLOBAL_MIN_PEERS);
            }
            
            globalConnectionPool = 
                new P2PConnectionPool<RobberConnection>(n); 
        } else {
            logger.info("no global connections needed");
            globalConnectionPool = null;
        }

        // print config settings
        logger.info("- local min. peers:      " + LOCAL_MIN_PEERS);
        logger.info("- global min. peers:     " + GLOBAL_MIN_PEERS);
        logger.info("- max. pending requests: " + 
                mcast.ht.bittorrent.Config.MAX_PENDING_REQUESTS);
    }

    private DirectedGraph<Collective> createCollectiveGraph(
            DirectedGraph<IbisIdentifier> g, Pool pool) {
        
        // create map of ibisidentifier to collective
        HashMap<IbisIdentifier, Collective> map = 
            new HashMap<IbisIdentifier, Collective>();
        
        for (Collective c: pool.getAllCollectives()) {
            for (IbisIdentifier id: c.getMembers()) {
                map.put(id, c);
            }
        }
        
        // create collective graph
        DirectedGraph<Collective> result = new DirectedGraph<Collective>();
        
        for (IbisIdentifier vertex: g.vertices()) {
            Collective vertexCollective = map.get(vertex);
            
            for (IbisIdentifier peer: g.outgoingNeighbors(vertex)) {
                Collective peerCollective = map.get(peer);
                result.addEdge(vertexCollective, peerCollective);
            }
        }
        
        return result;
    }
    
    private long generateSeed(List<IbisIdentifier> ibises) {
        long seed = 0;
        
        for (IbisIdentifier ibis: ibises) {
            seed += ibis.name().hashCode();
        }
        
        return seed;
    }
        
    public static List<PortType> getPortTypes() {
        List<PortType> result = new LinkedList<PortType>();
        
        result.add(RobberConnection.getPortType());
        result.add(Doorbell.getPortType());
        
        return result;
    }
    
    protected void doMulticastStorage(Storage storage, 
            Set<IbisIdentifier> roots, PieceIndexSet possession) 
            throws IOException {

        PieceIndexSet work = initWork(storage.getPieceCount());
        work.removeAll(possession);
        logger.info("my work is " + work.size() + " pieces: " + work);

        Collective myCollective = pool.getCollective(me);
        List<IbisIdentifier> myMembers = myCollective.getMembers();
        
        boolean doStealing = checkDoStealing(myMembers, roots);

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

    private boolean checkDoStealing(List<IbisIdentifier> myMembers, 
            Set<IbisIdentifier> roots) {
        if (globalConnectionPool == null) {
            // we have only one collective; no work stealing needed
            return false;
        } else if (roots == null) {
            // no set of root nodes supplied, which is the same as providing
            // an empty set of roots. Result: we have to do work stealing.
            return true;
        } else {
            return Collections.disjoint(myMembers, roots);
        }
    }
    
    protected void doFlush() throws IOException {
        if (globalConnectionPool != null) {
            globalConnectionPool.stop();
        }
        if (localConnectionPool != null) {
            localConnectionPool.stop();
        }
    }

    private PieceIndexSet initWork(int totalPieces) {
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
        String prop = BitTorrentConnection.MGMT_PROP_PIECED_RCVD;
        
        long piecesReceivedLocal = 0;
        if (localConnectionPool != null) { 
            localConnectionPool.getLongTotal(prop);
        }
        
        long piecesReceivedGlobal = 0;
        if (globalConnectionPool != null) {
            globalConnectionPool.getLongTotal(prop);
        }

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

    protected void doClose() throws IOException {
        if (globalConnectionPool != null) {
            globalConnectionPool.close();
        }
        if (localConnectionPool != null) {
            localConnectionPool.close();
        }
    }
    
    @Override
    public long getLongTotal(String mgmtProperty) {
        long localValue = 0;
        if (localConnectionPool != null) {
            localValue = localConnectionPool.getLongTotal(mgmtProperty);
        }
        
        long globalValue = 0;
        if (globalConnectionPool != null) {
            globalValue = globalConnectionPool.getLongTotal(mgmtProperty);
        }
        
        return localValue + globalValue;
    }

}
