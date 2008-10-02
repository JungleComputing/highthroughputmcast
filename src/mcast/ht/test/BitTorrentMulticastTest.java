package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.List;

import mcast.ht.MulticastChannel;
import mcast.ht.Pool;
import mcast.ht.bittorrent.BitTorrentMulticastChannel;

public class BitTorrentMulticastTest extends P2PMulticastTest {
    
    public BitTorrentMulticastTest(String name) {
        super(name);
    }
    
    public List<PortType> getPortTypes() {
        return BitTorrentMulticastChannel.getPortTypes();
    }
    
    protected MulticastChannel createChannel(Ibis ibis, Pool pool)
    throws IOException {
        return new BitTorrentMulticastChannel(ibis, pool);
    }

}
