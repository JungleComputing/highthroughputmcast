package mcast.ht;

public interface Config {

    static final String PROPERTY_PREFIX = "mcast.ht.";

	static final String s_random_seed = PROPERTY_PREFIX + "random_seed";

	static ConfigProperties config = ConfigProperties.getInstance();
	
	/**
	 * Seed value to use for Random instances in various algorithms
	 */
	static final int RANDOM_SEED = config.getIntProperty(s_random_seed, 23979);
	
}
