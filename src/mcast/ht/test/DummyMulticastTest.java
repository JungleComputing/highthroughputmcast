package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.IOException;
import java.util.Set;

import mcast.ht.Pool;
import mcast.ht.storage.Storage;

public class DummyMulticastTest implements MulticastTest {

    public String getName() {
        return "dummy";
    }

    public PortType getPortType() {
        return new PortType();
    }
    
    public void setUp(Ibis ibis, Pool pool) {
        // do nothing
    }
    
    public long[] timeMulticast(Storage storage, Set<IbisIdentifier> roots)
    throws IOException {
        // do nothing
        return new long[0];
    }

    public void printStats() throws IOException {
        // do nothing
    }

    public void close() throws IOException {
        // do nothing
    }

}
