package mcast.ht.bittorrent;

import mcast.ht.ConfigProperties;

import org.apache.log4j.Logger;

/**
 * @author mathijs
 */
public interface Config {

    public Logger statsLogger = Logger.getLogger("mcast.stats.bittorrent");

    static final String PROPERTY_PREFIX = 
        mcast.ht.Config.PROPERTY_PREFIX + "bittorrent.";

    static final String s_min_peers = 
        PROPERTY_PREFIX + "min_peers";
    static final String s_max_pending_requests = 
        PROPERTY_PREFIX + "max_pending_requests";
    static final String s_rate_estimate_period = 
        PROPERTY_PREFIX + "rate_estimate_period";
    static final String s_choking = 
        PROPERTY_PREFIX + "choking";
    static final String s_choking_interval = 
        PROPERTY_PREFIX + "choking_interval";
    static final String s_tit_for_tat_peers = 
        PROPERTY_PREFIX + "tit_for_tat_peers";
    static final String s_optimistic_unchoke_peers = 
        PROPERTY_PREFIX + "optimistic_unchoke_peers";
    static final String s_end_game = 
        PROPERTY_PREFIX + "end_game";

    static final ConfigProperties config = ConfigProperties.getInstance();

    /**
     * Minimum number of peers a node should connect to
     */
    static final int MIN_PEERS = config.getIntProperty(s_min_peers, 5);

    /**
     * Number of unchoked peers using the tit-for-tat choking algorithm
     */
    static final int TIT_FOR_TAT_PEERS = 
        config.getIntProperty(s_tit_for_tat_peers, 4);

    /**
     * Number of unchoked peers using the optimistic unchoke algorithm
     */
    static final int OPTIMISTIC_UNCHOKE_PEERS = 
        config.getIntProperty(s_optimistic_unchoke_peers, 1);

    /**
     * Interval between rechoking moments, in milliseconds
     */
    static final long CHOKING_INTERVAL = 
        config.getLongProperty(s_choking_interval, 10 * 1000);

    /**
     * Maximum number of pending requests on a connection to a peer
     */
    static final int MAX_PENDING_REQUESTS = 
        config.getIntProperty(s_max_pending_requests, 5);

    /**
     * The lenght of the rolling time frame over which to estimate the 
     * downloading rate, in milliseconds
     */
    static final int RATE_ESTIMATE_PERIOD = 
        config.getIntProperty(s_rate_estimate_period, 5000);

    /**
     * Whether to do choking or not (if not, the set of random chosen peers 
     * never changes during execution)
     */
    static final boolean CHOKING = config.getBooleanProperty(s_choking, false);

    /**
     * Whether to do end game mode
     */
    static final boolean END_GAME = config.getBooleanProperty(s_end_game, false);

}

