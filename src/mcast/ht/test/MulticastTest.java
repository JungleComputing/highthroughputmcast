package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.Set;

import mcast.ht.Pool;
import mcast.ht.storage.Storage;

public interface MulticastTest {

    public abstract String getName();
    
    public abstract PortType getPortType();
    
    public abstract void setUp(Ibis ibis, Pool pool) throws IOException;

    public abstract long[] timeMulticast(Storage storage, 
            Set<IbisIdentifier> roots) throws IOException;

    public abstract void printStats() throws IOException;
    
    public abstract void close() throws IOException;

}