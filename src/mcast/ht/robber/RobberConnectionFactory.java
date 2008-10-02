package mcast.ht.robber;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import mcast.ht.net.P2PConnectionFactory;

public class RobberConnectionFactory
implements P2PConnectionFactory<RobberConnection>
{

    private String poolName;
    
    public RobberConnectionFactory(String poolName) {
        this.poolName = poolName;
    }

    public PortType getPortType() {
        return RobberConnection.getPortType();
    }
    
    public RobberConnection createConnection(IbisIdentifier me, 
            IbisIdentifier peer) {
        return new RobberConnection(poolName, me, peer);
    }

}
