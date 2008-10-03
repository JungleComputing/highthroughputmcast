package clusteremulation;

import ibis.poolInfo.PoolInfo;
import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.direct.DirectSocketAddress;
import ibis.smartsockets.direct.IPAddressSet;
import ibis.smartsockets.hub.Hub;
import ibis.smartsockets.util.TypedProperties;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import mcast.ht.util.Defense;

import org.apache.log4j.Logger;

/**
 * Starts all the components of a network emulation within a single cluster.
 * 
 * @author mathijs
 */
public class ClusterEmulation implements Config {

    private static final String PREFIX_NETWORKS = "smartsockets.networks";
    private static final String PROP_NETWORKS_NAME = PREFIX_NETWORKS + ".name";
    //private static final String PROP_HUB_CLUSTERS = "smartsockets.hub.clusters";
    private static final String PROP_HUB_ADDRESSES = "smartsockets.hub.addresses";
    private static final String PROP_HUB_DISCOVERY_ALLOWED = "smartsockets.discovery.allowed";
    //private static final String PROP_CLUSTER_MEMBER = "smartsockets.cluster.member";
    //private static final String PROP_HUB_SEND_BUFFER = "smartsockets.hub.sendbuffer";
    //private static final String PROP_HUB_RECEIVE_BUFFER = "smartsockets.hub.receivebuffer";
    //private static final String PROP_SERVICELINK_SEND_BUFFER = "smartsockets.servicelink.sendbuffer";
    //private static final String PROP_SERVICELINK_RECEIVE_BUFFER = "smartsockets.servicelink.receivebuffer";
    private static final String PROP_HUBROUTED_BUFFER = "smartsockets.modules.hubrouted.size.buffer";
    private static final String PROP_HUB_SSH = "smartsockets.hub.ssh";
    //private static final String PROP_CONNECT_TIMEOUT = "smartsockets.timeout.connect";

    private static final String PREFIX_MODULES = "smartsockets.modules";
    private static final String PROP_MODULES_DEFINE = PREFIX_MODULES
    + ".define";

    private static final String NETWORK_VISUALIZATION = "viz";
    private static final String NETWORK_NAMESERVER = "ns";

    private static Logger logger = Logger.getLogger(ClusterEmulation.class);

    static {
        // print properties
        logger.info("Cluster Emulation - Hub port:           " + HUB_PORT);
        logger.info("Cluster Emulation - Network preference: " + NETWORK_PREFERENCE);
    }

    private final PoolInfo pool;
    private final int hostCount;
    
    // <hostCount> cluster names, which is less than pool.size(). The application
    // hosts can use their rank to look up their cluster name in this array.
    private final String[] clusterNames;    
    
    private final String myCluster;
    
    // ordered set of clusters; the ordering is identical on all nodes
    private final List<String> clusters;
    
    private final EmulationGauge gauge;
    private Hub myHub;
    private boolean meHub;

    /**
     * Creates a cluster emulation. The cluster layout is based on the clusters
     * defined in the given script.
     * 
     * For the emulation to work, the a priori environment should be as follows:
     * <ul>
     * <li>you must be able to execute Linux Traffic Control command on all
     * nodes running the emulation</li>
     * <li>the SmartSockets library may not have been initialized</li>
     * </ul>
     * 
     * The emulation observes the given script, and will emulate the environment
     * specified in the script. To prevent race conditions, the script should be
     * started after the emulation has been created.
     * 
     * @param script
     *                the script that specifies the emulated environment
     */
    public ClusterEmulation(PoolInfo pool, EmulationScript script)
    throws ParseException, IOException {
        Defense.checkNotNull(pool, "pool info");
        Defense.checkNotNull(script, "script");

        if (pool.size() != 
            script.getHostCount() + script.getClusterCount()) {
            throw new IllegalStateException("script defines "
                    + script.getHostCount() + " hosts + "
                    + script.getClusterCount() + " clusters != "
                    + pool.size() + " nodes");
        }

        this.pool = pool;
        
        clusterNames = script.getClusterNames();
        hostCount = clusterNames.length;
        
        // create an ordered list of all clusters
        SortedSet<String> clusterSet = new TreeSet<String>();
        for (String clusterName: clusterNames) {
            clusterSet.add(clusterName);
        }
        clusters = new ArrayList<String>(clusterSet);

        // create the gauge in which the script will store the values
        gauge = new EmulationGauge(clusterNames);
        script.addObserver(gauge);
        
        if (pool.rank() < script.getHostCount()) {
            // I'm an application node
            meHub = false;
            myCluster = clusterNames[pool.rank()];
            int myClusterIndex = clusters.indexOf(myCluster);            
            
            IPAddressSet myHubAddrSet = clusterHubAddrs(myClusterIndex);
            initSmartSocketsApplication(myHubAddrSet);

            logger.info("I am an application node in " + myCluster);
            logger.info("My hub is " + myHub + " (" + myHubAddrSet + ")");

            InetAddress[] myHubAddrs = myHubAddrSet.getAddresses();

            new NodeTrafficControl(gauge, EMULATE_BANDWIDTH, myHubAddrs, pool.rank());
        } else {
            // I'm a hub node
            meHub = true;
            myCluster = clusters.get(pool.rank() - hostCount); 
            
            logger.info("I am the hub for cluster " + myCluster);

            IPAddressSet myAddrSet = pool.getIPAddressSet();
            initSmartSocketsHub(myAddrSet);

            Map<String, InetAddress[]> clusterHubMap = new HashMap<String, InetAddress[]>();

            for (int i = 0; i < clusters.size(); i++) {
                String cluster = clusters.get(i);
                IPAddressSet hubAddrs = clusterHubAddrs(i);
                clusterHubMap.put(cluster, hubAddrs.getAddresses());
            }

            InetAddress[][] hostAddrs = new InetAddress[hostCount][];
            
            for (int i = 0; i < hostCount; i++) {
                hostAddrs[i] = pool.getIPAddressSet(i).getAddresses();
            }

            new HubTrafficControl(gauge, EMULATE_DELAY, EMULATE_BANDWIDTH, 
                    myCluster, clusterHubMap, hostAddrs);
        }
    }

    public int myRank() {
        return pool.rank();
    }
    
    public boolean meHub() {
        return meHub;
    }

    public EmulatedGauge getGauge() {
        return gauge;
    }

    public void end() {
        if (myHub != null) {
            logger.info("Ending hub");
            myHub.end();
        }
    }

    private void initSmartSocketsApplication(IPAddressSet myHubAddrSet) 
    throws UnknownHostException {
        logger.info("Initializing application node " + pool.rank() + 
                " in cluster " + myCluster);
        logger.info("Emulating node capacity: " + EMULATE_BANDWIDTH);

        String networkName, acceptedNetworks;

        if (FAST_LOCAL_NETWORK) {
            // A fast local network is 'emulated' by leting nodes in the same 
            // cluster communicate directly with each other (bypassing the route 
            // via the cluster's hub). This is done by giving all nodes and hub 
            // the same SmartSockets network name.
            networkName = clusterNetworkName(myCluster);
            acceptedNetworks = NETWORK_NAMESERVER;
        } else {
            // When the fast local network is disabled, all communication with 
            // local nodes should also go via the cluster's hub node. This is 
            // done by giving all nodes a different network name. The hub node 
            // of a cluster accept connections from all nodes in its cluster, 
            // and each node accepts connections from the cluster's hub node.
            networkName = nodeNetworkName(myCluster, pool.rank());
            acceptedNetworks = NETWORK_NAMESERVER + "," + 
                    clusterNetworkName(myCluster);
        }

        initSmartSocketsCommonProperties(networkName, acceptedNetworks);

        initProperty(PROP_NETWORKS_NAME, networkName);
        // initProperty(PROP_CLUSTER_MEMBER, myCluster);
        DirectSocketAddress hubAddrs = buildSocketAddress(myHubAddrSet);
        initProperty(PROP_HUB_ADDRESSES, hubAddrs.toString());

        logger.info("Hubrouted buffer size: " + HUBROUTED_BUFFER + " bytes");

        initProperty(PROP_HUBROUTED_BUFFER, Integer.toString(HUBROUTED_BUFFER));
        initProperty("smartsockets.modules.hubrouted.size.ack", 
                Integer.toString(HUBROUTED_BUFFER / 4));

        logger.info("Application node initialized");
    }

    private void initSmartSocketsHub(IPAddressSet myAddrSet) 
    throws UnknownHostException, IOException {
        logger.info("Initializing hub node " + 
                buildSocketAddress(myAddrSet) + 
                " for cluster " + myCluster);

        if (EMULATE_ON_HUBS) {
            logger.info("Emulating bandwidth: " + EMULATE_BANDWIDTH);
            logger.info("Emulating delay: " + EMULATE_DELAY);
        } else {
            logger.info("Emulation on hub nodes: DISABLED");
        }

        String networkName = clusterNetworkName(myCluster);
        String acceptedNetworks = clusterHubAcceptedNetworks();

        // the visualization can be attached to the hub for cluster 0
        if (clusters.indexOf(myCluster) == 0) {
            DirectSocketAddress vizAddr = DirectSocketAddress.getByAddress(
                    myAddrSet, HUB_PORT);
            logger.info("Visualization hook node address: " + vizAddr
                    + " / smartsockets.networks.name=" + NETWORK_VISUALIZATION);
            acceptedNetworks += "," + NETWORK_VISUALIZATION;
        }

        initSmartSocketsCommonProperties(networkName, acceptedNetworks);

        initProperty(PROP_NETWORKS_NAME, networkName);
        //initProperty(PROP_HUB_CLUSTERS, myCluster);
        initProperty(PROP_HUB_SSH, "false");
        //initProperty(PROP_HUB_SEND_BUFFER, Integer.toString(8 * 1024 * 1024));
        //initProperty(PROP_HUB_RECEIVE_BUFFER, Integer.toString(8 * 1024 * 1024));

        // We give our hub the addresses of all other hubs. We start with our
        // own address. The hub will filter this out and use the next one, but 
        // an Ibis that is created after the emulation will use the first 
        // address in the list (which is our address) and its virtual socket 
        // factory will connect to the local hub instead of a hub
        // on another node. This makes the visualisation more sensible.
        initProperty(PROP_HUB_ADDRESSES, allHubAddressesStartingWith(myAddrSet)); 

        TypedProperties p = SmartSocketsProperties.getDefaultProperties();

        myHub = new Hub(p);

        logger.info("Hub node initialized");
    }

    private String allHubAddressesStartingWith(IPAddressSet first) 
    throws UnknownHostException {
        StringBuilder result = new StringBuilder();

        DirectSocketAddress firstAddr = buildSocketAddress(first);  
        result.append(firstAddr.toString());

        for (int i = 0; i < clusters.size(); i++) {
            IPAddressSet s = clusterHubAddrs(i);

            if (!s.equals(first)) {
                DirectSocketAddress hubAddr = DirectSocketAddress.getByAddress(
                        s, HUB_PORT);

                result.append(',');
                result.append(hubAddr.toString());
            }
        }

        return result.toString();
    }

    private IPAddressSet clusterHubAddrs(int clusterIndex) {
        return pool.getIPAddressSet(hostCount + clusterIndex);
    }

    private String clusterHubAcceptedNetworks() {
        StringBuilder networks = new StringBuilder();

        networks.append(NETWORK_NAMESERVER);

        // we always accept the network of all other hubs
        for (String cluster: clusterNames) {
            if (!cluster.equals(myCluster)) {
                networks.append(',');
                String networkName = clusterNetworkName(cluster);
                networks.append(networkName);
            }
        }

        if (!FAST_LOCAL_NETWORK) {
            // if we do not emulate a fast local network, local nodes cannot 
            // connect directly to each other. This is enforced by placing each 
            // node in a different SmartSockets network. In that case, the hub 
            // of a cluster should accept all networks of all hosts in its own 
            // cluster.
            for (int i = 0; i < clusterNames.length; i++) {
                if (clusterNames[i].equals(myCluster)) {
                    String nodeNetworkName = nodeNetworkName(myCluster, i);
                    networks.append(',');
                    networks.append(nodeNetworkName);
                }
            }
        }

        return networks.toString();
    }

    private String clusterNetworkName(String clusterName) {
        return "hub" + clusterName;
    }

    private String nodeNetworkName(String clusterName, int nodeRank) {
        return "hub" + clusterName + "-node" + nodeRank;
    }

    private DirectSocketAddress buildSocketAddress(IPAddressSet addrSet)
    throws UnknownHostException
    {
        return DirectSocketAddress.getByAddress(addrSet, HUB_PORT);
    }

    private void initSmartSocketsCommonProperties(String networkName,
            String acceptedNetworks)
    {
        String propNetworksDefine = PREFIX_NETWORKS + ".define";
        String propFirewallDefault = PREFIX_NETWORKS + "." + networkName + 
                ".firewall.default";
        String propFirewallAccept = PREFIX_NETWORKS + "." + networkName +
                ".firewall.accept";
        String propPreferenceDefault = PREFIX_NETWORKS + "." + networkName +
                ".preference.default";

        initProperty(propNetworksDefine, networkName + "," + acceptedNetworks);
        initProperty(propFirewallDefault, "deny");
        initProperty(propFirewallAccept, acceptedNetworks);
        initProperty(propPreferenceDefault, NETWORK_PREFERENCE);

        initProperty(PROP_MODULES_DEFINE, "direct,hubrouted");
        initProperty(PROP_HUB_DISCOVERY_ALLOWED, "false");
        // initProperty(PROP_CONNECT_TIMEOUT, "10000");
        //initProperty(PROP_SERVICELINK_SEND_BUFFER, Integer.toString(8 * 1024 * 1024));
        //initProperty(PROP_SERVICELINK_RECEIVE_BUFFER, Integer.toString(8 * 1024 * 1024));
    }

    private void initProperty(String name, String value) {
        System.setProperty(name, value);
        logger.debug("- " + name + " = " + System.getProperty(name));
    }

}
