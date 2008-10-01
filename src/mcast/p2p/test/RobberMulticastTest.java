package mcast.p2p.test;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;

import java.io.IOException;

import mcast.p2p.MulticastChannel;
import mcast.p2p.Pool;
import mcast.p2p.robber.RobberMulticastChannel;

public class RobberMulticastTest extends P2PMulticastTest {
    
    public RobberMulticastTest(String name) {
        super(name);
    }
    
    public PortType getPortType() {
        return RobberMulticastChannel.getPortType();
    }
    
    protected MulticastChannel createChannel(Ibis ibis, Pool pool)
    throws IOException {
        return new RobberMulticastChannel(ibis, pool);
    }

}
