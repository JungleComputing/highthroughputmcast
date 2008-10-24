package mcast.ht.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import mcast.ht.graph.DirectedGraph;

import org.apache.log4j.Logger;

public class GraphConnectionNegotiator<C extends P2PConnection>
implements P2PConnectionNegotiator<C>
{

    private Logger logger = Logger.getLogger(GraphConnectionNegotiator.class);

    private Collection<C> connections;

    public GraphConnectionNegotiator(DirectedGraph<IbisIdentifier> peerGraph, 
            Ibis ibis, P2PConnectionFactory<C> connectionFactory)
    throws IOException
    {
        connections = new ArrayList<C>();

        logger.info("Creating connection pool from graph");

        IbisIdentifier me = ibis.identifier();
        
        // create a connection object for each of my peers
        for (IbisIdentifier peer : peerGraph.outgoingNeighbors(me)) {
            logger.debug("- creating connection for peer " + peer);

            C connection = connectionFactory.createConnection(me, peer);
            connections.add(connection);
        }

        // enable incoming connections for each peer
        for (C c : connections) {
            logger.debug("- enabling incoming connections from " + c.getPeer());
            c.enableConnect(ibis);
        }

        // connect to each peer
        for (C c : connections) {
            logger.debug("- connecting to " + c.getPeer());
            c.connect(ibis);
        }
    }

    public Iterator<C> iterator() {
        return connections.iterator();
    }

}
