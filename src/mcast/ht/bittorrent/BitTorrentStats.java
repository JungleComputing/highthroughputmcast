package mcast.ht.bittorrent;

import java.io.IOException;

import mcast.ht.bittorrent.BitTorrentAdmin;
import mcast.ht.bittorrent.BitTorrentConnection;
import mcast.ht.net.P2PConnection;
import mcast.ht.net.P2PConnectionPool;

import org.apache.log4j.Logger;

public class BitTorrentStats {

    public static void printStats(Logger l, String prefix,
            P2PConnectionPool<? extends P2PConnection> connections,
            BitTorrentAdmin admin) 
    throws IOException
    {
        if (connections != null) {
            connections.printStats(prefix);
        }

        if (admin != null) {
            int totalPieces = admin.getNoTotalPieces();
            logPieceOrigin(l, prefix + "stats_", connections, totalPieces);
            logBytes(l, prefix + "stats_", connections);
            
            admin.printStats(prefix);
        }
    }
    
    protected static void logPieceOrigin(Logger l, String prefix,
            P2PConnectionPool<? extends P2PConnection> connections,
            int totalPieces)
    {
        if (l.isInfoEnabled()) {
            String prop = BitTorrentConnection.MGMT_PROP_PIECES_RCVD; 
            
            long piecesReceived = 0;
            if (connections != null) {
                piecesReceived = connections.getLongTotal(prop);
            }
            
            double total = (double)totalPieces;
            double percPiecesRcvd = piecesReceived / total * 100;
            
            String f = prefix + "pool_pieces_rcvd %1$d = %2$.2f %%";
            String msg = String.format(f, piecesReceived, percPiecesRcvd); 
            
            l.info(msg);
        }
    }
    
    protected static void logBytes(Logger l, String prefix,
            P2PConnectionPool<? extends P2PConnection> connections)
    {
        if (l.isInfoEnabled() && connections != null) {
            long bytesSent = connections.getLongTotal("BytesSent");
            l.info(prefix + "bytes_sent: " + bytesSent);
        
            long bytesReceived = connections.getLongTotal("BytesReceived");
            l.info(prefix + "bytes_rcvd: " + bytesReceived);
            
            connections.setManagementProperty("BytesSent", 0);
            connections.setManagementProperty("BytesReceived", 0);
        }
    }

}
