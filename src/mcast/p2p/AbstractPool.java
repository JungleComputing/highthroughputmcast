package mcast.p2p;

import ibis.ipl.IbisIdentifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPool implements Pool {

    protected String name;
    protected IbisIdentifier[] ibises;
    
    public AbstractPool(String name, IbisIdentifier[] ibises) {
        this(name, ibises.length);
        System.arraycopy(ibises, 0, this.ibises, 0, ibises.length);
    }
    
    public AbstractPool(String name, int size) {
        this.name = name;
        this.ibises = new IbisIdentifier[size];
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
        String name = collectiveName(ibis);
        List<IbisIdentifier> members = collectiveMembers(ibis);
        
        return new Collective(name, members);
    }

    protected abstract String collectiveName(IbisIdentifier id);
    
    protected abstract List<IbisIdentifier> collectiveMembers(IbisIdentifier ibis);
    
    @Override
    public List<IbisIdentifier> getEverybody() {
        return Collections.unmodifiableList(Arrays.asList(ibises));
    }

}
