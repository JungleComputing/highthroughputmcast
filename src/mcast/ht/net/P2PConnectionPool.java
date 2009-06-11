package mcast.ht.net;

import java.io.IOException;
import java.util.Iterator;

import mcast.ht.admin.P2PAdmin;
import mcast.ht.storage.Storage;

import org.apache.log4j.Logger;

public class P2PConnectionPool<C extends P2PConnection> implements Iterable<C> {

    private Logger logger = Logger.getLogger(P2PConnectionPool.class);

    private P2PConnectionNegotiator<C> negotiator;

    long runtimeMillis;
    
    public P2PConnectionPool(P2PConnectionNegotiator<C> negotiator) {
        this.negotiator = negotiator;
        runtimeMillis = 0;
    }

    public void close() throws IOException {
        synchronized (negotiator) {
            for (C connection : negotiator) {
                connection.close();
            }
        }
    }

    public void printStats(String prefix) {
        synchronized (negotiator) {
            // print stats of each connection separately
            for (C connection : negotiator) {
                connection.printStats(prefix, runtimeMillis);
            }
        }
    }

    public void init(Storage storage, P2PAdmin admin) throws IOException { 
        runtimeMillis = System.currentTimeMillis();
        
        synchronized (negotiator) {
            // first, initialize each connection
            for (C connection : negotiator) {
                logger.debug("initializing connection to " + connection.getPeer());
                connection.init(storage, admin);
            }

            // second, add all connections as a received message
            // listener
            for (C connection : negotiator) {
                logger.debug("adding connection to " + connection.getPeer() + " to admin");
                admin.addConnection(connection);
            }
        }
    }

    public void start() throws IOException {
        synchronized (negotiator) {
            for (C connection : negotiator) {
                connection.start();
            }
        }
    }

    public void stop() throws IOException {
        synchronized (negotiator) {
            for (C connection : negotiator) {
                connection.stop();
            }
        }
        runtimeMillis = System.currentTimeMillis() - runtimeMillis;
    }

    public Iterator<C> iterator() {
        return negotiator.iterator();
    }
    
    public long getLongTotal(String mgmtProperty) {
        long result = 0;

        synchronized(negotiator) {
            for (C connection : negotiator) {
                Number n = connection.getManagementProperty(mgmtProperty); 
                result += n.longValue();
            }
        }

        return result;
    }

    public void setManagementProperty(String key, Number value) {
        synchronized(negotiator) {
            for (C connection : negotiator) {
                connection.setManagementProperty(key, value);
            }
        }
    }

}
