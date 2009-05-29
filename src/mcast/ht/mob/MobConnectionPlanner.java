package mcast.ht.mob;

import mcast.ht.graph.AllOtherPeersGenerator;
import mcast.ht.graph.DirectedGraph;
import mcast.ht.graph.DirectedGraphFactory;
import mcast.ht.graph.PossiblePeersGenerator;
import mcast.ht.Collective;
import mcast.ht.Pool;

import ibis.ipl.IbisIdentifier;

import java.util.List;

import org.apache.log4j.Logger;

public class MobConnectionPlanner {

    private static Logger logger = Logger.getLogger(MobConnectionPlanner.class);

    private final DirectedGraph<IbisIdentifier> localConnections;
    private final DirectedGraph<IbisIdentifier> globalConnections;

    public MobConnectionPlanner(Pool pool, IbisIdentifier me, int localMinPeers, 
            int globalMinPeers, int retries,
            PossiblePeersGenerator<IbisIdentifier> globalPeersGenerator)
    {
        localConnections = new DirectedGraph<IbisIdentifier>();
        
        boolean logConnections = false;
        if (logger.isDebugEnabled()) {
            // let host with rank 0 log the local and global connections
            List<IbisIdentifier> everybody = pool.getEverybody();
            logConnections = !everybody.isEmpty() && everybody.get(0).equals(me);
        }
        
        // create all local connections
        for (Collective c : pool.getAllCollectives()) {
            List<IbisIdentifier> members = c.getMembers();
            if (members.size() > 1) {
                logger.info("- planning local connections in cluster " + c);

                DirectedGraph<IbisIdentifier> clusterConnectionGraph = 
                    createLocalRandomConnections(c, me, localMinPeers, retries);

                localConnections.addGraph(clusterConnectionGraph);
            } else {
                logger.info("- no local connections needed in cluster " + c);
            }
        }

        if (logConnections) {
            logger.debug("local connections:\n" + localConnections);
        }

        // create all global connection
        if (pool.getAllCollectives().size() > 1) {
            logger.info("- creating global connections");

            globalConnections = createGlobalRandomConnections(pool, me,
                    localConnections, globalMinPeers, retries, 
                    globalPeersGenerator);

        } else {
            logger
            .info("- no global connections needed: my cluster is the only one");
            globalConnections = new DirectedGraph<IbisIdentifier>();
        }

        if (logConnections) {
            logger.debug("global connections:\n" + globalConnections);
        }
    }

    private DirectedGraph<IbisIdentifier> createLocalRandomConnections(
            Collective c, IbisIdentifier me, int minPeers, int retries)
    {
        long seed = createSeed(c);

        PossiblePeersGenerator<IbisIdentifier> allOtherPeers = 
            new AllOtherPeersGenerator<IbisIdentifier>(c.getMembers());

        if (logger.isDebugEnabled()) {
            List<IbisIdentifier> l = allOtherPeers.generatePossiblePeers(me); 
            logger.debug("possible local peers: " + l);
        }

        for (int i = 0; i < retries; i++) {
            DirectedGraph<IbisIdentifier> result = 
                DirectedGraphFactory.createMinDegreeRandomGraph(c.getMembers(),
                        minPeers, seed, allOtherPeers);

            if (result.isWeaklyConnected()) {
                return result;
            }
        }

        throw new InternalError("could not create random local connections" +
                " (minDegree=" + minPeers + ")");
    }

    private DirectedGraph<IbisIdentifier> createGlobalRandomConnections(
            Pool pool, IbisIdentifier me, 
            DirectedGraph<IbisIdentifier> allLocalConnectionGraph, 
            int minPeers, int retries, 
            PossiblePeersGenerator<IbisIdentifier> peersGenerator)
    {
        long seed = createSeed(pool);

        if (logger.isDebugEnabled()) {
            List<IbisIdentifier> l = peersGenerator.generatePossiblePeers(me);
            logger.debug("possible global peers: " + l); 
        }
        
        for (int i = 0; i < retries; i++) {
            DirectedGraph<IbisIdentifier> result = 
                DirectedGraphFactory.createMinDegreeRandomGraph(
                        pool.getEverybody(), minPeers, seed, peersGenerator);

            if (allCollectivesEqualSize(pool)) {
                // no need to check for connectedness, since the graph
                // will almost surely be connected anyway
                return result;
            } else {
                // check if the global connection + all local connections
                // result in a connected MOB graph
                DirectedGraph<IbisIdentifier> test = 
                    new DirectedGraph<IbisIdentifier>(result);
                test.addGraph(allLocalConnectionGraph);

                if (MobSimulator.isMulticastConnected(pool, test)) {
                    return result;
                }
            }
        }

        throw new InternalError(
                "could not create random global connections (minDegree: "
                + minPeers + ")");
            }

    private boolean allCollectivesEqualSize(Pool pool) {
        int size = -1;

        for (Collective c: pool.getAllCollectives()) {
            if (size == -1) {
                size = c.getMembers().size();
            } else if (c.getMembers().size() != size) {
                return false;
            }
        }

        return true;
    }

    private long createSeed(Collective c) {
        // for reproducibility of measurements, generate a static seed
        return c.getName().hashCode();
    }
    
    private long createSeed(Pool p) {
        // for reproducibility of measurements, generate a static seed
        return p.getName().hashCode();
    }

    public DirectedGraph<IbisIdentifier> getLocalConnections() {
        return localConnections;
    }

    public DirectedGraph<IbisIdentifier> getGlobalConnections() {
        return globalConnections;
    }

}
