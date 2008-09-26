package mcast.p2p;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Pool where the collectives are based on the location of the Ibis identifiers.
 */
public class LocationPool implements Pool {

    private String name;
    private IbisIdentifier[] ibises;
    
    public LocationPool(String name, IbisIdentifier[] everybody) {
        this.name = name;
        
        ibises = new IbisIdentifier[everybody.length];
        System.arraycopy(everybody, 0, ibises, 0, everybody.length);
        
        Arrays.sort(ibises, new LocationComparator());
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Collection<Collective> getAllCollectives() {
       Collection<Collective> result = new LinkedList<Collective>();
       LinkedList<IbisIdentifier> l = new LinkedList<IbisIdentifier>();
       
       for (IbisIdentifier ibis: ibises) {
           l.add(ibis);
       }
       
       while (!l.isEmpty()) {
           IbisIdentifier ibis = l.getFirst();
           Collective collective = getCollective(ibis);
           result.add(collective);
           l.removeAll(collective.getMembers());
       }
       
       return result;
    }

    @Override
    public Collective getCollective(IbisIdentifier ibis) {
        List<IbisIdentifier> members = new ArrayList<IbisIdentifier>();
        
        Location targetLocation = ibis.location();
        
        for (int i = 0; i < ibises.length; i++) {
            Location location = ibises[i].location();
            
            if (sameCollective(targetLocation, location)) {
                members.add(ibises[i]);
            }
        }
        
        String collectiveName = collectiveName(targetLocation);
        
        return new LocationCollective(collectiveName, members);
    }

    private boolean sameCollective(Location loc1, Location loc2) {
        int levels1 = loc1.numberOfLevels();
        int levels2 = loc2.numberOfLevels();
        
        if (levels1 != levels2) {
            return false;
        }
        
        return loc1.numberOfMatchingLevels(loc2) >= levels1 - 1;
    }
    
    private String collectiveName(Location location) {
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
    
    @Override
    public List<IbisIdentifier> getEverybody() {
        return Arrays.asList(ibises);
    }

    
    // INNER CLASSES
    
    private class LocationComparator implements Comparator<IbisIdentifier> {

        @Override
        public int compare(IbisIdentifier ibis1, IbisIdentifier ibis2) {
            return ibis1.location().compareTo(ibis2.location());
        }
                
    }
    
}
