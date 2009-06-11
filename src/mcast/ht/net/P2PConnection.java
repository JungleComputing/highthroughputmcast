package mcast.ht.net;

import ibis.ipl.Ibis;

import java.io.IOException;

import mcast.ht.admin.P2PAdmin;
import mcast.ht.storage.Storage;

public interface P2PConnection {

    public void enableConnect(Ibis ibis) throws IOException;

    public void connect(Ibis ibis) throws IOException;

    public void init(Storage storage, P2PAdmin admin);

    public void start() throws IOException;

    public void stop();

    public void close() throws IOException;

    public Object getPeer();

    public Number getManagementProperty(String key);

    public void setManagementProperty(String key, Number value);

    public void printStats(String prefix, long totalTimeMillis);

}
