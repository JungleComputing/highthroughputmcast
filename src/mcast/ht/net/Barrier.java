package mcast.ht.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

public class Barrier {

    private static Logger logger = Logger.getLogger(Barrier.class);
    
    public static void sync(Collection<IbisIdentifier> ibises, Ibis ibis) 
            throws IOException {
        
        IbisIdentifier me = ibis.identifier();
        Registry r = ibis.registry();
        
        logger.debug("announcing myself");
        r.elect(me.name());
        
        for (IbisIdentifier id: ibises) {
            if (!id.equals(me)) {
                String idName = id.name();
                
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for " + idName);
                }
                
                r.getElectionResult(idName);
            }
        }
    }
    
}
