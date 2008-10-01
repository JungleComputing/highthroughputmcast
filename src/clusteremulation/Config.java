package clusteremulation;

public interface Config {

    static final String PREFIX_TS = "trafficshaper.";

    static final String s_shape_latency = PREFIX_TS + "latency";
    static final String s_shape_bandwidth = PREFIX_TS + "bandwidth";
    static final String s_shape_local_capacity = PREFIX_TS + "local_capacity";

    static final String PREFIX_CE = "clusteremulation.";

    static final String s_emulate_delay = PREFIX_CE + "delay";
    static final String s_emulate_bandwidth = PREFIX_CE + "bandwidth";
    static final String s_emulate_on_hubs = PREFIX_CE + "emulate_on_hubs";
    static final String s_hub_port = PREFIX_CE + "hub_port";
    static final String s_tell_port = PREFIX_CE + "tell_port";
    static final String s_tcp_sendbuffer = PREFIX_CE + "tcp_sendbuffer";
    static final String s_tcp_receivebuffer = PREFIX_CE + "tcp_receivebuffer";
    static final String s_hubrouted_buffer = PREFIX_CE + "hubrouted_buffer";
    static final String s_network_preference = PREFIX_CE + "network_preference";
    static final String s_fast_local_network = PREFIX_CE + "fast_local_network";
    static final String s_random_seed = PREFIX_CE + "random_seed";

    static ConfigProperties config = ConfigProperties.getInstance();
    
//    // simulate latency with the traffic shaper?
//    static final boolean SHAPE_LATENCY = 
//        config.getBooleanProperty(s_shape_latency, false);
//
//    // simulate WAN bandwidth with the traffic shaper?
//    static final boolean SHAPE_BANDWIDTH = 
//        config.getBooleanProperty(s_shape_bandwidth, false);
//
//    // simulate local capacity (total incoming/outgoing bandwidth per node) with
//    // the traffic shaper?
//    static final boolean SHAPE_LOCAL_CAPACITY = 
//        config.getBooleanProperty(s_shape_local_capacity, false);
//
    // emulate latency with the cluster emulator
    static final boolean EMULATE_DELAY = 
        config.getBooleanProperty(s_emulate_delay, false);

    // emulate bandwidth with the cluster emulator
    static final boolean EMULATE_BANDWIDTH = 
        config.getBooleanProperty(s_emulate_bandwidth, false);

    // use emulation on the hub nodes of the cluster emulator
    static final boolean EMULATE_ON_HUBS = 
        config.getBooleanProperty(s_emulate_on_hubs, true);
    
      // port of the SmartSockets hubs
      static final int HUB_PORT = config.getIntProperty(s_hub_port, 17878);
//
//    // size of the TCP send buffers (in bytes)
//    static final int TCP_SENDBUFFER = 
//        config.getIntProperty(s_tcp_sendbuffer, 1024 * 1024 * 8);
//
//    // size of the TCP receive buffers (in bytes)
//    static final int TCP_RECEIVEBUFFER = 
//        config.getIntProperty(s_tcp_receivebuffer, 1024 * 1024 * 8);
//
      // size of the SmartSockets hubrouted buffers (in bytes)
      static final int HUBROUTED_BUFFER = 
          config.getIntProperty(s_hubrouted_buffer, 1024 * 1024 * 16);

      // network preference for the SmartSockets
      static final String NETWORK_PREFERENCE = 
          config.getStringProperty(s_network_preference, "10.153.0.0/255.255.255.0,global");

      // whether the emulation lets all hosts in the same cluster communicate 
      // directly, without any traffic shaping
      static final boolean FAST_LOCAL_NETWORK = 
          config.getBooleanProperty(s_fast_local_network, false);

      // seed value to use for the random generator in the emulation script
      static final int RANDOM_SEED = config.getIntProperty(s_random_seed, 23979);

}
