package mcast.p2p;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Pool where the collectives are based on the location of the Ibis identifiers.
 */
public class LocationPool extends AbstractPool implements Pool {

    public LocationPool(String name, IbisIdentifier[] everybody) {
        super(name, everybody);
        
        Arrays.sort(ibises, new LocationComparator());
    }
    
    @Override
    protected List<IbisIdentifier> collectiveMembers(IbisIdentifier ibis) {
        List<IbisIdentifier> members = new ArrayList<IbisIdentifier>();
        
        for (int i = 0; i < ibises.length; i++) {
            if (sameCollective(ibis, ibises[i])) {
                members.add(ibises[i]);
            }
        }
        
        return members;
    }

    private boolean sameCollective(IbisIdentifier id1, IbisIdentifier id2) {
        Location loc1 = id1.location();
        Location loc2 = id2.location();
        
        int levels1 = loc1.numberOfLevels();
        int levels2 = loc2.numberOfLevels();
        
        if (levels1 != levels2) {
            return false;
        }
        
        return loc1.numberOfMatchingLevels(loc2) >= levels1 - 1;
    }
    
    @Override
    protected String collectiveName(IbisIdentifier id) {
        Location location = id.location();
        int levelCount = location.numberOfLevels(); 
        
        if (levelCount == 1) {
            return "single_collective";
        } else {
            StringBuilder b = new StringBuilder();
            
            for (int i = 1; i < levelCount; i++) {
                if (i > 1) {
                    b.append('@');
                }
                b.append(location.getLevel(i));
            }
            
            return b.toString();
        }
    }
    
    // INNER CLASSES
    
    private class LocationComparator implements Comparator<IbisIdentifier> {

        @Override
        public int compare(IbisIdentifier ibis1, IbisIdentifier ibis2) {
            return ibis1.location().compareTo(ibis2.location());
        }
                
    }
    
}
