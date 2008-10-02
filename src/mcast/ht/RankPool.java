package mcast.ht;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.Registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pool where the collectives are based on the names of the clusters.
 */
public class RankPool extends AbstractPool implements Pool {

    private String[] collectiveNames;
    
    public RankPool(String name, Ibis ibis, String[] collectiveNames, int myRank) 
    throws IOException {
        super(name, collectiveNames.length);
        
        this.collectiveNames = new String[collectiveNames.length];
        System.arraycopy(collectiveNames, 0, this.collectiveNames, 0, 
                collectiveNames.length);
        
        Registry registry = ibis.registry();
        
        // elect each rank to determine the associated Ibis identifier
        if (myRank >= 0 && myRank < collectiveNames.length) {
            String myRankElection = Integer.toString(myRank);
            registry.elect(myRankElection);
        }

        for (int i = 0; i < collectiveNames.length; i++) {
            String rankElection = Integer.toString(i);
            ibises[i] = registry.getElectionResult(rankElection);
        }
    }

    @Override
    protected String collectiveName(IbisIdentifier id) {
        int rank = getRank(id);
        return collectiveNames[rank];
    }

    private int getRank(IbisIdentifier id) {
        for (int i = 0; i < ibises.length; i++) {
            if (ibises[i].equals(id)) {
                return i;
            }
        }
        throw new RuntimeException("Unexpected error: could not find the " +
        		"rank of " + id);
        
    }
    
    @Override
    protected List<IbisIdentifier> collectiveMembers(IbisIdentifier ibis) {
        List<IbisIdentifier> members = new ArrayList<IbisIdentifier>();
        String collectiveName = collectiveName(ibis);
        
        for (int i = 0; i < ibises.length; i++) {
            if (collectiveNames[i].equals(collectiveName)) {
                members.add(ibises[i]);
            }
        }
        
        return members;
    }

}
