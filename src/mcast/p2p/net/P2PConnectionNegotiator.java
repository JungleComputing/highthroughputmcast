package mcast.p2p.net;

import java.util.Iterator;

public interface P2PConnectionNegotiator<C extends P2PConnection> 
extends Iterable<C> {

    public Iterator<C> iterator();

}
