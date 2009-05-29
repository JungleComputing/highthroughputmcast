package mcast.ht.mob;

import ibis.ipl.IbisIdentifier;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import mcast.ht.graph.DirectedGraph;
import mcast.ht.Collective;
import mcast.ht.Pool;

public class MobSimulator {

    private static Logger logger = Logger.getLogger(MobSimulator.class);

    public static boolean isMulticastConnected(Pool pool, 
            DirectedGraph<IbisIdentifier> connections) 
    {
        // first, test if the nodes in each cluster forms a connect graph
        // N.B. this is not really necessary when each node has at least 3 
        // neighbors... (see thesis)
        for (Collective c : pool.getAllCollectives()) {
            HashSet<IbisIdentifier> clusterIbisIdentifiers = 
                new HashSet<IbisIdentifier>(c.getMembers());
            
            DirectedGraph<IbisIdentifier> clusterConnections = 
                connections.subgraph(clusterIbisIdentifiers);

            if (!clusterConnections.isWeaklyConnected()) {
                return false;
            }
        }

        // second, try each host as a multicast root, 
        // and simulate if data is distributed to all hosts
        List<Collective> allCollectives = pool.getAllCollectives();
        
        for (IbisIdentifier root : pool.getEverybody()) {

            // create simulation pieces for each cluster 
            int noTestPieces = leastCommonMultipleClusterSize(pool);
            BitSet[] pieces = new BitSet[allCollectives.size()];
            
            Collective rootCollective = pool.getCollective(root);
            int rootCollectiveRank = allCollectives.indexOf(rootCollective); 
            
            for (int i = 0; i < pieces.length; i++) {
                pieces[i] = new BitSet();

                if (i == rootCollectiveRank) {
                    pieces[i].set(0, noTestPieces);
                }
            }

            printPieces(pieces);

            // flood the data from the root while obeying mob shares
            boolean changed = true;

            while (changed) {
                changed = false;

                for (IbisIdentifier h : pool.getEverybody()) {
                    Collective hCollective = pool.getCollective(h);
                    int hCollectiveRank = allCollectives.indexOf(hCollective);
                    
                    for (IbisIdentifier peer : connections.outgoingNeighbors(h)) {
                        Collective peerCollective = pool.getCollective(peer);
                        int peerCollectiveRank = allCollectives.indexOf(peerCollective);
                            
                        if (!peerCollective.equals(hCollective)) {
                            // peer is a global peer; give him all pieces of 
                            // h that are part of peer's mob share

                            BitSet hostPieces = pieces[hCollectiveRank];
                            BitSet peerPieces = pieces[peerCollectiveRank];

                            int peerPieceCount = peerPieces.cardinality();
                            MobShare peerMobShare = new MobShare(peerCollective, 
                                    peer, noTestPieces);

                            BitSet mobShareMask = new BitSet();
                            mobShareMask.set(peerMobShare.getFirstPieceIndex(), 
                                    peerMobShare.getLastPieceIndex() + 1);

                            BitSet gift = (BitSet) hostPieces.clone();
                            gift.and(mobShareMask);

                            peerPieces.or(gift);

                            if (logger.isDebugEnabled()) {
                                logger.debug(h + " -> " + peer + ": " + gift + 
                                        " (peer mob share: " + peerMobShare + ")");
                            }

                            if (peerPieces.cardinality() > peerPieceCount) {
                                changed = true;
                                printPieces(pieces);
                            }
                        }
                    }
                }
            }

            // check if each cluster received all data
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i].cardinality() < noTestPieces) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void printPieces(BitSet[] pieces) {
        if (logger.isDebugEnabled()) {
            logger.debug("");

            for (int i = 0; i < pieces.length; i++) {
                logger.debug(i + ": " + pieces[i]);
            }

            logger.debug("");
        }
    }

    private static int leastCommonMultipleClusterSize(Pool pool) {
        int result = 1;

        for (Collective c : pool.getAllCollectives()) {
            int size = c.getMembers().size();
            if (size > result) {
                result = lcm(result, size);
            }
        }

        return result;
    }

    // least common multiple
    private static int lcm(int a, int b) {
        return (a / gcd(a, b)) * b;
    }

    // greatest common divisor
    private static int gcd(int a, int b) {
        int m = a % b;

        if (m == 0) {
            return b;
        } else {
            return gcd(b, m);
        }
    }

}
