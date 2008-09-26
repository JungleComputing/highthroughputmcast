package mcast.p2p.net;

import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mcast.p2p.Config;

import org.apache.log4j.Logger;

public class IndividualConnectionNegotiator<C extends P2PConnection>
implements P2PConnectionNegotiator<C>, DoorbellHandler, Config
{

    private static Logger logger = 
            Logger.getLogger(IndividualConnectionNegotiator.class);

    private volatile Set<IbisIdentifier> peers;
    private volatile String name;
    private Collection<C> connections;
    private Doorbell doorbell;

    public IndividualConnectionNegotiator(String name,
            Collection<IbisIdentifier> ibises, Ibis ibis, 
            P2PConnectionFactory<C> connectionFactory, int minPeers)
            throws IOException {
        
        this.name = name;

        logger.info("creating individual connection negotiator called \""
                + name + "\"");

        // first, create the connection administration variables
        peers = Collections.synchronizedSet(new HashSet<IbisIdentifier>());

        // second, activate our doorbell, so peers can start initiating
        // connections with us
        doorbell = new Doorbell(ibis, name, this);
        doorbell.activate();

        // create a deterministic random seed 
        int seed = RANDOM_SEED + name.hashCode();
        
        // choose peers
        logger.debug("choosing peers");
        List<IbisIdentifier> peerOptions = new ArrayList<IbisIdentifier>(ibises);
        peerOptions.remove(ibis.identifier());
        choosePeers(peerOptions, minPeers, seed);

        logger.debug("syncing");
        Barrier.sync(ibises, ibis);

        logger.info(name + "connecting to peers " + peers);
        connections = createConnections(peers, ibis, connectionFactory);
    }

    public Iterator<C> iterator() {
        return connections.iterator();
    }

    private void choosePeers(List<IbisIdentifier> peerOptions, 
            int minPeers, int seed) throws IOException {
        
        Random random = new Random(seed);
        int desiredPeerCount = Math.min(minPeers, peerOptions.size());

        List<IbisIdentifier> failedPeers = new ArrayList<IbisIdentifier>();

        while (peers.size() < desiredPeerCount) {
            // if all peers have been tried, we start retrying the failed peers
            if (peerOptions.isEmpty()) {
                logger.info("all peers have been tried, retrying failed ones: "
                        + failedPeers.toString());

                peerOptions.addAll(failedPeers);
                failedPeers.clear();
            }

            // choose a random peer to connect to
            int choiceIndex = random.nextInt(peerOptions.size());
            IbisIdentifier peer = peerOptions.remove(choiceIndex);

            // There's a race condition here between peers.contains(peer) and 
            // peer.add(peer), which does not matter since peers is a set and
            // will therefore not contain duplicate items in case two nodes
            // simultenously select each other as peers. This if-construct is
            // merely an optimization that saves a superfluous doorbell.ring().
            if (!peers.contains(peer)) {
                try {
                    doorbell.ring(peer);
                    peers.add(peer);
                } catch (ConnectionTimedOutException e) {
                    logger.warn("could not ring the doorbell of " + peer, e);
                    failedPeers.add(peer);
                }
            } else {
                // peer already connected to us: keep it and continue
            }
        }
    }

    private Collection<C> createConnections(Set<IbisIdentifier> peers, 
            Ibis ibis, P2PConnectionFactory<C> connectionFactory) 
            throws IOException {
        
        Collection<C> connections = new ArrayList<C>(peers.size());
        IbisIdentifier me = ibis.identifier();
        
        // 1. create all connections (and receive ports)
        for (IbisIdentifier peer : peers) {
            logger.info("creating connection with " + peer);
            C connection = connectionFactory.createConnection(me, peer);
            connection.enableConnect(ibis);
            connections.add(connection);
        }

        // 2. connect to all peers
        for (P2PConnection c: connections) {
            logger.info(name + "connecting to " + c.getPeer());
            c.connect(ibis);
        }

        return connections;
    }

    public boolean answerDoorbell(IbisIdentifier applicant) {
        logger.debug(name + " doorbell rang by " + applicant);
        peers.add(applicant);
        return true;
    }

}
