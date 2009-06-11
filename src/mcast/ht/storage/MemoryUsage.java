package mcast.ht.storage;

import mcast.ht.util.Convert;

import org.apache.log4j.Logger;

public class MemoryUsage {

    /**
     * The maximum amount of memory (in bytes) to use for cached pieces
     */
    public static final long MAX = 
        (int)Math.max(1024 * 1024 * 100, Runtime.getRuntime().maxMemory() * 0.9);

    static {
        Logger logger = Logger.getLogger(WriteCache.class);
        double maxMB = Convert.bytesToMBytes(MAX);
        logger.info(String.format("max. memory: %1$.2f MB", maxMB));  
    }

    /**
     * The estimated total amount of memory (in bytes) used by various storage classes
     */
    public static volatile long used = 0;	

}
