package mcast.p2p;

import ibis.ipl.IbisIdentifier;

import java.util.List;

public interface Collective {

    public String getName();
    
    public List<IbisIdentifier> getMembers();
    
}
