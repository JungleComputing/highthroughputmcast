package mcast.p2p.bittorrent;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import mcast.p2p.net.P2PConnectionFactory;

public class BitTorrentConnectionFactory
implements P2PConnectionFactory<BitTorrentConnection>, Config {

    private String poolName;
    
    public BitTorrentConnectionFactory(String poolName) {
        this.poolName = poolName; 
    }

    public PortType getPortType() {
        return BitTorrentConnection.getPortType();
    }
    
    public BitTorrentConnection createConnection(IbisIdentifier me, 
            IbisIdentifier peer) {
        return new BitTorrentConnection(poolName, me, peer, 
                CHOKING, CHOKING, CHOKING);
    }

}
