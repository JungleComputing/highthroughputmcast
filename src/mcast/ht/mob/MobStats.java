package mcast.ht.mob;

import java.io.IOException;

import mcast.ht.bittorrent.BitTorrentAdmin;
import mcast.ht.bittorrent.BitTorrentStats;
import mcast.ht.net.P2PConnection;
import mcast.ht.net.P2PConnectionPool;

import org.apache.log4j.Logger;

public class MobStats extends BitTorrentStats {

    public static void printStats(Logger l, String prefix,
            P2PConnectionPool<? extends P2PConnection> localConnections,
            P2PConnectionPool<? extends P2PConnection> globalConnections,
            BitTorrentAdmin admin) 
    throws IOException
    {
        if (localConnections != null) {
            localConnections.printStats(prefix);
        }

        if (globalConnections != null) {
            globalConnections.printStats(prefix);
        }

        if (admin != null) {
            int t = admin.getNoTotalPieces();
            logPieceOrigin(l, prefix + "stats_local_", localConnections, t);
            logPieceOrigin(l, prefix + "stats_global_", localConnections, t);

            logBytes(l, prefix + "stats_local_", localConnections);
            logBytes(l, prefix + "stats_global_", globalConnections);
            
            admin.printStats(prefix);
        }
    }
    
}
