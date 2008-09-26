package mcast.p2p.net;

import ibis.ipl.Ibis;

import java.io.IOException;

import mcast.p2p.admin.P2PAdmin;
import mcast.p2p.storage.Storage;

public interface P2PConnection {

    public void enableConnect(Ibis ibis) throws IOException;

    public void connect(Ibis ibis) throws IOException;

    public void init(Storage storage, P2PAdmin admin);

    public void start() throws IOException;

    public void stop();

    public void close() throws IOException;

    public Object getPeer();

    public int getPiecesReceived();

    public int getPiecesSent();

    public void printStats(long totalTimeMillis);

}
