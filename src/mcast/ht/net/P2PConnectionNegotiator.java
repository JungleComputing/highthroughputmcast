package mcast.ht.net;

import java.util.Iterator;

public interface P2PConnectionNegotiator<C extends P2PConnection> 
extends Iterable<C> {

    public Iterator<C> iterator();

}
