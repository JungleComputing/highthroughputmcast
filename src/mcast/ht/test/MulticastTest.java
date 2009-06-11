package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import mcast.ht.MulticastChannel;
import mcast.ht.Pool;
import mcast.ht.storage.Storage;

public interface MulticastTest {

    public String getName();
    
    public List<PortType> getPortTypes();
    
    public void setUp(Ibis ibis, Pool pool) throws IOException;

    public long[] timeMulticast(Storage storage, 
            Set<IbisIdentifier> roots) throws IOException;

    public MulticastChannel getChannel();
    
    public void close() throws IOException;

}