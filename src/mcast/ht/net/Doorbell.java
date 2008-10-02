package mcast.ht.net;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * @author mathijs
 */
public class Doorbell implements ReceivePortConnectUpcall {

    private static Logger logger = Logger.getLogger(Doorbell.class);

    private static PortType portType = new PortType(
            PortType.COMMUNICATION_FIFO, 
            PortType.COMMUNICATION_RELIABLE,
            PortType.CONNECTION_MANY_TO_ONE, 
            PortType.CONNECTION_UPCALLS,
            PortType.RECEIVE_EXPLICIT, 
            PortType.SERIALIZATION_OBJECT);
    
    private Object me;
    private Ibis ibis;
    private String name;
    private DoorbellHandler doorbellHandler;
    private ReceivePort rport;

    public Doorbell(Ibis ibis, String name, DoorbellHandler doorbellHandler) 
            throws IOException {
        this.ibis = ibis;
        this.name = "doorbell-" + name;
        this.doorbellHandler = doorbellHandler;

        if (logger.isDebugEnabled())
            logger.debug("creating doorbell " + name);
        
        rport = ibis.createReceivePort(portType, name, this);
    }

    public static PortType getPortType() {
        return portType;
    }
    
    public void activate() {
        rport.enableConnections();
    }

    public synchronized boolean ring(IbisIdentifier peer) throws IOException, 
            ConnectionTimedOutException
    {
        logger.debug("ringing doorbell of host " + peer + "...");
        
        String myName = me.toString();
        
        SendPort peerDoorbell = ibis.createSendPort(portType, myName);
        
        try {
            peerDoorbell.connect(peer, name);
            logger.debug(peer + " accepted our doorbell ring");
            peerDoorbell.close();
            return true;
        } catch (ConnectionRefusedException e) {
            logger.debug(peer + " refused our doorbell ring");
            return false;
        }
    }

    public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
        return doorbellHandler.answerDoorbell(applicant.ibisIdentifier());
    }

    public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe,
            Throwable reason) {
        // ignore
    }

    public void close() throws IOException {
        rport.close();
    }

}
