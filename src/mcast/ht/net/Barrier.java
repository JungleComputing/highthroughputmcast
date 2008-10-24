package mcast.ht.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

public class Barrier {

    private static Logger logger = Logger.getLogger(Barrier.class);
    
    private String name;
    private int count;
    
    public Barrier(String name) {
        this.name = name;
        count = 0;
    }
    
    private String createBallot(IbisIdentifier id) {
        return name + "-" + id.name() + "-" + count;
    }
    
    public synchronized void sync(Collection<IbisIdentifier> ibises, 
            Ibis ibis) throws IOException {
        
        IbisIdentifier me = ibis.identifier();
        Registry r = ibis.registry();
        
        logger.debug("announcing myself");
        String myBallot = createBallot(me);
        r.elect(myBallot);
        
        for (IbisIdentifier id: ibises) {
            if (!id.equals(me)) {
                String idBallot = createBallot(id);
                
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting for " + idBallot);
                }
                
                r.getElectionResult(idBallot);
            }
        }
        
        count++;
    }
    
}
