package mcast.p2p.storage;

import mcast.p2p.util.Convert;

import org.apache.log4j.Logger;

public class MemoryUsage {

    /**
     * The maximum amount of memory (in bytes) to use for cached pieces
     */
    public static final long MAX = 
        (int)Math.max(1024 * 1024 * 100, Runtime.getRuntime().maxMemory() * 0.9);

    static {
        Logger logger = Logger.getLogger(WriteCache.class);
        logger.info("max. memory: " + 
                Convert.round(Convert.bytesToMBytes(MAX), 2) + " MB");
    }

    /**
     * The estimated total amount of memory (in bytes) used by various storage classes
     */
    public static volatile long used = 0;	

}
