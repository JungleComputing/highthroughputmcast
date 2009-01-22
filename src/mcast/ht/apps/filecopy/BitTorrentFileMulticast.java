package mcast.ht.apps.filecopy;


import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.Registry;

import java.util.List;

import mcast.ht.MulticastChannel;
import mcast.ht.bittorrent.BitTorrentMulticastChannel;

public class BitTorrentFileMulticast extends AbstractFileMulticast {

    public BitTorrentFileMulticast(boolean deleteOnExit) throws Exception {
        super(deleteOnExit);
    }

    protected PortType[] getPortTypes() {
        List<PortType> l = BitTorrentMulticastChannel.getPortTypes();
        return l.toArray(new PortType[0]);   
    }
    
    protected MulticastChannel createMulticastChannel(Ibis ibis) 
    throws Exception 
    {
        // wait until everybody joined 
        Registry registry = ibis.registry();
        registry.waitUntilPoolClosed();
        
        // create multicast channel
        IbisIdentifier[] everybody = registry.joinedIbises();
        return new BitTorrentMulticastChannel(ibis, everybody, "BitTorrentFileCopy");
    }

    public String getImplementationName() {
        return "BitTorrentFileMulticast";
    }

}
