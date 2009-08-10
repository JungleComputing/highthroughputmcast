package mcast.ht.robber;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mcast.ht.Collective;
import mcast.ht.Config;
import mcast.ht.Pool;
import mcast.ht.graph.PossiblePeersGenerator;

public class GlobalPeersGenerator 
implements PossiblePeersGenerator<IbisIdentifier> {

    private Pool pool;
    private Random random;
    
    public GlobalPeersGenerator(Pool pool) {
        this.pool = pool;
        
        random = new Random(Config.RANDOM_SEED);
    }

    public List<IbisIdentifier> generatePossiblePeers(IbisIdentifier node) {
        Collective nodeCollective = pool.getCollective(node);
        
        List<IbisIdentifier> nodeMembers = nodeCollective.getMembers();
        int nodeCollectiveRank = nodeMembers.indexOf(node);

        List<IbisIdentifier> result = new ArrayList<IbisIdentifier>();

        for (Collective peerCollective: pool.getAllCollectives()) {
            if (!peerCollective.equals(nodeCollective)) {
                List<IbisIdentifier> peerMembers = peerCollective.getMembers();
                
                double peerCollectiveShare = 
                    peerMembers.size() / (double)nodeMembers.size();
                
                int firstPeerRank = (int)Math.floor(nodeCollectiveRank * peerCollectiveShare);
                int lastPeerRank = (int)Math.ceil((nodeCollectiveRank + 1) * peerCollectiveShare);
                
                IbisIdentifier peerOption = null;
                
                if (firstPeerRank == lastPeerRank) {
                    // do not call the random generator
                    peerOption = peerMembers.get(firstPeerRank);
                } else {
                    // choose a random rank between first (inclusive) and last (exclusive)
                    int r = random.nextInt(lastPeerRank - firstPeerRank);
                    peerOption = peerMembers.get(firstPeerRank + r);
                }
                
                result.add(peerOption);
            }
        }

        return result;
    }

}
