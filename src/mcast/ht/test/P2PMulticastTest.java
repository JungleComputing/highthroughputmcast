package mcast.ht.test;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Set;

import mcast.ht.MulticastChannel;
import mcast.ht.Pool;
import mcast.ht.storage.Storage;

public abstract class P2PMulticastTest implements MulticastTest {

    private final String name;
    protected MulticastChannel channel;
    protected AckChannel ackChannel;

    public P2PMulticastTest(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setUp(Ibis ibis, Pool pool) throws IOException {
        channel = createChannel(ibis, pool);
        ackChannel = new AckChannel(ibis, pool);
    }
    
    protected abstract MulticastChannel createChannel(Ibis ibis, Pool pool) 
    throws IOException;
    
    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public long[] timeMulticast(Storage storage, Set<IbisIdentifier> roots)
            throws IOException {
        long[] result = new long[2];

        // make sure all nodes are ready
        ackChannel.acknowledge();

        // time multicast
        long start = System.nanoTime();

        channel.multicastStorage(storage, roots);

        long received = System.nanoTime();

        channel.flush();

        // sync all nodes afterwards
        ackChannel.acknowledge();

        long end = System.nanoTime();

        result[0] = received - start;  // time until we received everything
        result[1] = end - start;       // time until everybody received everything

        return result;
    }

    @Override
    public void printStats() throws IOException {
        channel.printStats();
    }

    @Override
    public String toString() {
        return name;
    }

}
