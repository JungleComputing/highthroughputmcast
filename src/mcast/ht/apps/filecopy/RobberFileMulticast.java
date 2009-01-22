package mcast.ht.apps.filecopy;


import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.Registry;

import java.util.List;

import org.apache.log4j.Logger;

import mcast.ht.Collective;
import mcast.ht.LocationPool;
import mcast.ht.MulticastChannel;
import mcast.ht.robber.RobberMulticastChannel;

public class RobberFileMulticast extends AbstractFileMulticast {

    private Logger logger = Logger.getLogger(RobberFileMulticast.class);
    
    public RobberFileMulticast(boolean deleteOnExit) throws Exception {
        super(deleteOnExit);
    }

    protected PortType[] getPortTypes() {
        List<PortType> l = RobberMulticastChannel.getPortTypes();
        return l.toArray(new PortType[0]);   
    }
    
    protected MulticastChannel createMulticastChannel(Ibis ibis) 
    throws Exception 
    {
        // wait until everybody joined 
        Registry registry = ibis.registry();
        registry.waitUntilPoolClosed();
        
        // create multicast channel
        IbisIdentifier[] everybody = registry.joinedIbises();
        LocationPool pool = new LocationPool("RobberFileCopy", everybody);
        
        logger.info("Pool used in Robber:");
        for (Collective c: pool.getAllCollectives()) {
            logger.info(c + ": " + c.getMembers());
        }
                
        return new RobberMulticastChannel(ibis, pool);
    }

    public String getImplementationName() {
        return "RobberFileMulticast";
    }

}
