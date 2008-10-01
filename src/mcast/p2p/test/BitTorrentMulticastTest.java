package mcast.p2p.test;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;

import java.io.IOException;

import mcast.p2p.MulticastChannel;
import mcast.p2p.Pool;
import mcast.p2p.bittorrent.BitTorrentMulticastChannel;

public class BitTorrentMulticastTest extends P2PMulticastTest {
    
    public BitTorrentMulticastTest(String name) {
        super(name);
    }
    
    public PortType getPortType() {
        return BitTorrentMulticastChannel.getPortType();
    }
    
    protected MulticastChannel createChannel(Ibis ibis, Pool pool)
    throws IOException {
        return new BitTorrentMulticastChannel(ibis, pool);
    }

}
