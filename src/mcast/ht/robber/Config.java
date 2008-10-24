package mcast.ht.robber;

import mcast.ht.ConfigProperties;

import org.apache.log4j.Logger;

interface Config {

    public Logger statsLogger = Logger.getLogger("mcast.stats.robber");

    static final String PROPERTY_PREFIX = 
        mcast.ht.Config.PROPERTY_PREFIX + "robber.";

    static final String s_local_min_peers = PROPERTY_PREFIX + "local_min_peers";
    static final String s_global_min_peers = PROPERTY_PREFIX + "global_min_peers";
    static final String s_work_dictator = PROPERTY_PREFIX + "work_dictator";
    static final String s_balance_booty = PROPERTY_PREFIX + "balance_booty";
    static final String s_stealing = PROPERTY_PREFIX + "stealing";
    static final String s_test_connected = PROPERTY_PREFIX + "test_connected";

    static ConfigProperties config = ConfigProperties.getInstance();
    
    // minimum number of peers in its own cluster a node should connect
    static final int LOCAL_MIN_PEERS = 
        config.getIntProperty(s_local_min_peers, 5);

    // minimum number of peers in other clusters a node should connect to
    static final int GLOBAL_MIN_PEERS = 
        config.getIntProperty(s_global_min_peers, 5);

    // whether to balanced to number of stolen pieces based on the amount of
    // pieces the stealer and the stolen have already received
    static final boolean BALANCE_BOOTY = 
        config.getBooleanProperty(s_work_dictator, false);

    // whether to do stealing
    static final boolean STEALING = 
        config.getBooleanProperty(s_stealing, true);

    // whether to generate a deterministic random communication graph, and
    // test this graph for connectedness. If false, the communication graph
    // is constructed in a distributed fashion, with each node connecting to
    // five neighbors chosen uniformly at random. The resulting communication
    // graphs are very likely to be connected, but it's not a 100% guarantee.
    static final boolean TEST_CONNECTED = 
        config.getBooleanProperty(s_test_connected, false);

}
