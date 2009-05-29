package mcast.ht.mob;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import mcast.ht.Pool;
import mcast.ht.net.P2PConnectionFactory;


public class MobConnectionFactory 
implements P2PConnectionFactory<MobConnection>
{
    private final Pool pool;

    public MobConnectionFactory(Pool pool) {
        this.pool = pool;
    }
    
    public PortType getPortType() {
        return MobConnection.getPortType();
    }
    
    public MobConnection createConnection(IbisIdentifier me, 
            IbisIdentifier peer) 
    {
        return new MobConnection(me, peer, pool);
    }

}
