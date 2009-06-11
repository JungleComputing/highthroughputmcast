package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.List;

import mcast.ht.MulticastChannel;
import mcast.ht.Pool;
import mcast.ht.mob.MobMulticastChannel;

public class MobMulticastTest extends P2PMulticastTest {
    
    public MobMulticastTest(String name) {
        super(name);
    }
    
    public List<PortType> getPortTypes() {
        return MobMulticastChannel.getPortTypes();
    }
    
    protected MulticastChannel createChannel(Ibis ibis, Pool pool)
    throws IOException {
        return new MobMulticastChannel(ibis, pool);
    }

}
