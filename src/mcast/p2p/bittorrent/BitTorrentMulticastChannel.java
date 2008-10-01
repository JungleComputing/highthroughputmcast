package mcast.p2p.bittorrent;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;

import mcast.p2p.AbstractMulticastChannel;
import mcast.p2p.LocationPool;
import mcast.p2p.Pool;
import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.net.IndividualConnectionNegotiator;
import mcast.p2p.net.P2PConnectionNegotiator;
import mcast.p2p.net.P2PConnectionPool;
import mcast.p2p.storage.Storage;

import org.apache.log4j.Logger;

public class BitTorrentMulticastChannel extends AbstractMulticastChannel
        implements Config {

    private static Logger logger = 
        Logger.getLogger(BitTorrentMulticastChannel.class);

    private final P2PConnectionNegotiator<BitTorrentConnection> connectionNegotiator;
    private final P2PConnectionPool<BitTorrentConnection> connectionPool;
    private final Timer chokingTimer;
    private BitTorrentAdmin admin;
    private TitForTatChoker choker;

    public BitTorrentMulticastChannel(Ibis ibis, IbisIdentifier[] members,
            String name) 
            throws IOException {
        
        this(ibis, new LocationPool(name, members));
    }
    
    public BitTorrentMulticastChannel(Ibis ibis, Pool pool) throws IOException {
        super(ibis.identifier());
        
        logger.info("creating BitTorrent multicast channel");
        
        connectionNegotiator = 
            new IndividualConnectionNegotiator<BitTorrentConnection>(
                "bittorrent", pool.getEverybody(), ibis, 
                new BitTorrentConnectionFactory(pool.getName()), MIN_PEERS);

        connectionPool = 
            new P2PConnectionPool<BitTorrentConnection>(connectionNegotiator);

        if (logger.isInfoEnabled()) {
            String info = "peers of " + ibis + ": ";
            String concat = "";

            for (BitTorrentConnection c: connectionNegotiator) {
                info += concat + c.getPeer();
                concat = ", ";
            }

            logger.info(info);
        }

        admin = null;

        chokingTimer = CHOKING ? new Timer("ChokingTimer", true) : null;
        choker = null;

        // print config settings
        logger.info("- min. peers:            " + MIN_PEERS);
        logger.info("- max. pending requests: " + MAX_PENDING_REQUESTS);
        logger.info("- end game:              " + END_GAME);
    }
    
    public static PortType getPortType() {
        return BitTorrentConnection.getPortType();
    }

    protected void doMulticastStorage(Storage storage, 
            Set<IbisIdentifier> roots, PieceIndexSet possession) 
            throws IOException {
        admin = new BitTorrentAdminImpl(storage.getPieceCount(), possession);

        connectionPool.init(storage, admin);
        connectionPool.start();

        if (CHOKING) {
            choker = new TitForTatChoker(admin, connectionNegotiator);
            chokingTimer.scheduleAtFixedRate(choker, 0, CHOKING_INTERVAL);
        }

        admin.waitUntilAllPiecesReceived();
    }

    protected void doFlush() throws IOException {
        connectionPool.stop();

        if (CHOKING) {
            logger.info("stopping choker...");
            choker.end();
        }
    }

    protected void doClose() throws IOException {
        connectionPool.close();
    }

    public synchronized void printStats() throws IOException {
        if (admin != null) {
            connectionPool.printStats();
            admin.printStats();
        }
    }

}
