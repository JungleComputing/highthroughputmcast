package mcast.p2p;

import ibis.ipl.IbisIdentifier;

import java.util.List;

public class LocationCollective implements Collective {

    private String name;
    private List<IbisIdentifier> members;
    
    public LocationCollective(String name, List<IbisIdentifier> members) {
        this.name = name;
        this.members = members;
    }
    
    @Override
    public List<IbisIdentifier> getMembers() {
        return members;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof LocationCollective) {
            LocationCollective rhs = (LocationCollective)o;
            return name.equals(rhs.name);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
    
}
