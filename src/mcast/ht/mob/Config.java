package mcast.ht.mob;

import org.apache.log4j.Logger;

import mcast.ht.ConfigProperties;

interface Config {

    public Logger statsLogger = Logger.getLogger("mcast.stats.mob");
    
    static final String PROPERTY_PREFIX = 
        mcast.ht.Config.PROPERTY_PREFIX + "mob.";

    static final String s_local_min_peers = PROPERTY_PREFIX + "local_min_peers";
    static final String s_global_min_peers = PROPERTY_PREFIX + "global_min_peers";
	static final String s_random_connect_attempts = PROPERTY_PREFIX + "random_connect_attempts";

	// minimum number of peers in its own cluster a node should connect
	static final int LOCAL_MIN_PEERS = 
	    ConfigProperties.getInstance().getIntProperty(s_local_min_peers, 5);

	// minimum number of peers in other clusters a node should connect to
	static final int GLOBAL_MIN_PEERS = 
	    ConfigProperties.getInstance().getIntProperty(s_global_min_peers, 5);

	// number of times to retry creating connected random graph (local and gloablly)
	static final int RANDOM_CONNECT_ATTEMPTS = 
	    ConfigProperties.getInstance().getIntProperty(s_random_connect_attempts, 10);
	
}
