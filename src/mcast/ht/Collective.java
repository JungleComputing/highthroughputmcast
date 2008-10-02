package mcast.ht;

import ibis.ipl.IbisIdentifier;

import java.util.List;

public class Collective {

    private String name;
    private List<IbisIdentifier> members;
    
    public Collective(String name, List<IbisIdentifier> members) {
        this.name = name;
        this.members = members;
    }
    
    public List<IbisIdentifier> getMembers() {
        return members;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof Collective) {
            Collective rhs = (Collective)o;
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
