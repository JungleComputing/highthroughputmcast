package mcast.ht.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

public interface P2PConnectionFactory<C extends P2PConnection> {

    public PortType getPortType();
    
    public C createConnection(IbisIdentifier me, IbisIdentifier peer);

}

    

