package mcast.ht.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

public class Barrier {

    private static Logger logger = Logger.getLogger(Barrier.class);
    
    private static int barrierCount = 0;
    
    public static synchronized void sync(Collection<IbisIdentifier> ibises, 
            Ibis ibis) throws IOException {
        
        IbisIdentifier me = ibis.identifier();
        Registry r = ibis.registry();
        
        logger.debug("announcing myself");
        r.elect(me.name() + '-' + barrierCount);
        
        for (IbisIdentifier id: ibises) {
            if (!id.equals(me)) {
                String idName = id.name() + '-' + barrierCount;
                
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for " + idName);
                }
                
                r.getElectionResult(idName);
            }
        }
        
        barrierCount++;
    }
    
}
