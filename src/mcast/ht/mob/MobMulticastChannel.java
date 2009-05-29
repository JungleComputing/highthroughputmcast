package mcast.ht.mob;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.graph.DirectedGraph;
import mcast.ht.graph.PossiblePeersGenerator;
import mcast.ht.AbstractMulticastChannel;
import mcast.ht.LocationPool;
import mcast.ht.bittorrent.BitTorrentAdmin;
import mcast.ht.bittorrent.BitTorrentAdminImpl;
import mcast.ht.bittorrent.BitTorrentConnection;
import mcast.ht.Pool;
import mcast.ht.net.GraphConnectionNegotiator;
import mcast.ht.net.P2PConnection;
import mcast.ht.net.P2PConnectionFactory;
import mcast.ht.net.P2PConnectionPool;
import mcast.ht.robber.GlobalPeersGenerator;
import mcast.ht.storage.Storage;

import mcast.ht.util.Convert;

public class MobMulticastChannel extends AbstractMulticastChannel
implements Config 
{

	private static Logger logger = Logger.getLogger(MobMulticastChannel.class);

    protected final Pool pool;
	private P2PConnectionPool<MobConnection> localConnectionPool;
	private P2PConnectionPool<MobConnection> globalConnectionPool;
	private BitTorrentAdmin admin;

	public MobMulticastChannel(Ibis ibis, IbisIdentifier[] members,
            String name) throws IOException {
        this(ibis, new LocationPool(name, members));
    }
    
    public MobMulticastChannel(Ibis ibis, Pool pool) throws IOException { 
		super(ibis.identifier());

        this.pool = pool;
		
		logger.info("Creating MOB multicast channel");

		PossiblePeersGenerator<IbisIdentifier> globalPeersGenerator = 
		        new GlobalPeersGenerator(pool);
		P2PConnectionFactory<MobConnection> connectionFactory = 
		        new MobConnectionFactory(pool);

		MobConnectionPlanner planner = new MobConnectionPlanner(pool, 
		        ibis.identifier(), LOCAL_MIN_PEERS, GLOBAL_MIN_PEERS, 
		        RANDOM_CONNECT_ATTEMPTS, globalPeersGenerator);

		DirectedGraph<IbisIdentifier> localConnections = 
		    planner.getLocalConnections();
		
		if (localConnections.noEdges() > 0) {
			logger.info("- creating local connections in my cluster");

			GraphConnectionNegotiator<MobConnection> localConnectionNegotiator = 
			    new GraphConnectionNegotiator<MobConnection>(localConnections, 
			            ibis, connectionFactory);

			localConnectionPool = new P2PConnectionPool<MobConnection>(
					localConnectionNegotiator);

			if (logger.isInfoEnabled()) {
			    logger.info("- local peers of " + ibis + ": " + 
			            peerList(localConnectionPool));
			}
		} else if (logger.isInfoEnabled()) {
			logger.info("- no local connections needed in cluster" + 
			        pool.getCollective(ibis.identifier()));
		}

		DirectedGraph<IbisIdentifier> globalConnections = 
		    planner.getGlobalConnections();
		
		if (globalConnections.noEdges() > 0) {
			logger.info("- creating global connections");
			
			GraphConnectionNegotiator<MobConnection> globalConnectionNegotiator = 
			    new GraphConnectionNegotiator<MobConnection>(globalConnections, 
			            ibis, connectionFactory);
			
			globalConnectionPool = 
			    new P2PConnectionPool<MobConnection>(globalConnectionNegotiator);

			if (logger.isInfoEnabled()) {
			    logger.info("- global peers of " + ibis + ": " + 
			            peerList(globalConnectionPool));
			}
		} else {
			logger.info("- no global connections needed: my cluster is the only one");
		}

		admin = null;

		// print config settings
		logger.debug("- done");

		logger.info("- local min. peers:      " + LOCAL_MIN_PEERS);
		logger.info("- global min. peers:     " + GLOBAL_MIN_PEERS);
		logger.info("- max. pending requests: " + mcast.ht.bittorrent.Config.MAX_PENDING_REQUESTS);
		logger.info("- end game:              "	+ mcast.ht.bittorrent.Config.END_GAME);
	}

	private String peerList(P2PConnectionPool<? extends P2PConnection> connectionPool) {
		StringBuilder peers = new StringBuilder();
		String concat = "";

		for (P2PConnection c: connectionPool) {
			peers.append(concat);
			peers.append(c.getPeer().toString());
			concat = ", ";
		}

		return peers.toString();
	}

	@Override
	protected void doMulticastStorage(Storage storage, 
	        Set<IbisIdentifier> roots, PieceIndexSet possession)
	throws IOException
	{
		admin = createAdmin(storage, possession);

		if (localConnectionPool != null) {
			localConnectionPool.init(storage, admin);
		}
		if (globalConnectionPool != null) {
			globalConnectionPool.init(storage, admin);
		}

		if (localConnectionPool != null) {
			localConnectionPool.start();
		}
		if (globalConnectionPool != null) {
			globalConnectionPool.start();
		}

		admin.waitUntilAllPiecesReceived();
	}

	protected BitTorrentAdmin createAdmin(Storage storage,
			PieceIndexSet possession)
	{
		return new BitTorrentAdminImpl(storage.getPieceCount(), possession);
	}

	@Override
	protected void doFlush()
	throws IOException
	{
		if (globalConnectionPool != null) {
			globalConnectionPool.stop();
		}
		if (localConnectionPool != null) {
			localConnectionPool.stop();
		}
	}

	@Override
	protected void doClose() 
	throws IOException 
	{
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
	
	public synchronized void printStats() throws IOException {
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
		
	    long piecesReceivedLocal = 
		    localConnectionPool.getLongTotal(prop);
		
		long piecesReceivedGlobal = 
		    globalConnectionPool.getLongTotal(prop);

		double totalPieces = admin.getNoTotalPieces();

		String percReceivedLocal = Convert.round((double) piecesReceivedLocal
				/ totalPieces * 100, 2);
		String percReceivedGlobal = Convert.round((double) piecesReceivedGlobal
				/ totalPieces * 100, 2);

		Config.statsLogger.info("pool_stats rcvd " + "local " + 
		        piecesReceivedLocal + " = " + percReceivedLocal + "% " + 
		        "global " + piecesReceivedGlobal + " = " + percReceivedGlobal + 
		        "%");
	}

}
