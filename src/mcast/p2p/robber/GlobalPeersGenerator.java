package mcast.p2p.robber;

import ibis.ipl.IbisIdentifier;

import java.util.ArrayList;
import java.util.List;

import mcast.p2p.Collective;
import mcast.p2p.Pool;

public class GlobalPeersGenerator 
implements PossiblePeersGenerator<IbisIdentifier> {

    private Pool pool;

    public GlobalPeersGenerator(Pool pool) {
        this.pool = pool;
    }

    public List<IbisIdentifier> generatePossiblePeers(IbisIdentifier node) {
        Collective nodeCollective = pool.getCollective(node);
        
        List<IbisIdentifier> nodeMembers = nodeCollective.getMembers();
        int nodeCollectiveRank = nodeMembers.indexOf(node);

        List<IbisIdentifier> result = new ArrayList<IbisIdentifier>();

        for (Collective peerCollective: pool.getAllCollectives()) {
            if (!peerCollective.equals(nodeCollective)) {
                List<IbisIdentifier> peerMembers = peerCollective.getMembers();
                
                int peerRank = nodeCollectiveRank % peerMembers.size();
                IbisIdentifier peerOption = peerMembers.get(peerRank);

                result.add(peerOption);
            }
        }

        return result;
    }

}
