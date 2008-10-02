package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.List;

import mcast.ht.MulticastChannel;
import mcast.ht.Pool;
import mcast.ht.robber.RobberMulticastChannel;

public class RobberMulticastTest extends P2PMulticastTest {
    
    public RobberMulticastTest(String name) {
        super(name);
    }
    
    public List<PortType> getPortTypes() {
        return RobberMulticastChannel.getPortTypes();
    }
    
    protected MulticastChannel createChannel(Ibis ibis, Pool pool)
    throws IOException {
        return new RobberMulticastChannel(ibis, pool);
    }

}
