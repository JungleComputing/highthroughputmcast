package mcast.ht;

import ibis.ipl.IbisIdentifier;

import java.util.List;

public interface Pool {

    public String getName();
    
    public List<IbisIdentifier> getEverybody();
    
    public Collective getCollective(IbisIdentifier member);
    
    public List<Collective> getAllCollectives();

}
