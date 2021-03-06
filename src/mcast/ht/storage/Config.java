package mcast.ht.storage;

import mcast.ht.ConfigProperties;

interface Config {

    static final String PROPERTY_PREFIX = 
        mcast.ht.Config.PROPERTY_PREFIX + "storage.";
    static final String s_max_open_files = PROPERTY_PREFIX + "max_open_files";
    
    static ConfigProperties config = ConfigProperties.getInstance(); 
    
	// maximum number of open files used by the storage implementations
	static final long MAX_OPEN_FILES = 
	    config.getIntProperty(s_max_open_files, 1000);
	
}
