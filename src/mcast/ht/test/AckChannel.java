package mcast.ht.test;

import ibis.ipl.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import mcast.ht.Pool;

import org.apache.log4j.Logger;

/**
 * Barrier-like acknowledgement operation using a binary tree.
 */
public class AckChannel {

    private static Logger logger = Logger.getLogger(AckChannel.class);

    public static final PortType PORT_TYPE_ACK = new PortType(
            PortType.COMMUNICATION_FIFO,
            PortType.CONNECTION_ONE_TO_MANY,
            PortType.RECEIVE_EXPLICIT,
            PortType.SERIALIZATION_BYTE);

    public static final PortType PORT_TYPE_ACK_ACK = new PortType(
            PortType.COMMUNICATION_FIFO,
            PortType.CONNECTION_MANY_TO_ONE,
            PortType.RECEIVE_EXPLICIT,
            PortType.SERIALIZATION_BYTE);

    private static Map<String, Integer> instanceCounterMap = 
        new HashMap<String, Integer>();

    private final IbisIdentifier parent;
    private final List<IbisIdentifier> children;
    private ReceivePort parentRport, childrenRport;
    private SendPort parentSport, childrenSport;
    private boolean synced;

    /**
     * 
     * @param ibis
     * @param pool
     * @param name a unique name within this Ibis instance
     * @throws IOException
     */
    public AckChannel(Ibis ibis, Pool pool) throws IOException {
        logger.info("Creating binary tree ack channel");

        List<IbisIdentifier> everybody = pool.getEverybody();
        IbisIdentifier me = ibis.identifier();
        
        int myIndex = everybody.indexOf(me);

        // determine parent
        int parentIndex = (int) Math.floor((myIndex - 1) / 2);
        parent = myIndex > 0 ? everybody.get(parentIndex) : null;

        // determine children
        int leftChildIndex = (myIndex * 2) + 1;
        int rightChildIndex = leftChildIndex + 1;
        children = new ArrayList<IbisIdentifier>(2);

        if (leftChildIndex >= 0 && leftChildIndex < everybody.size()) {
            children.add(everybody.get(leftChildIndex));
        }

        if (rightChildIndex >= 0 && rightChildIndex < everybody.size()) {
            children.add(everybody.get(rightChildIndex));
        }

        String poolName = pool.getName();
        
        int instanceCount = 0;
        try {
            instanceCount = instanceCounterMap.get(pool.getName());
        } catch (NullPointerException ignored) {
            // first instance
        }
               
        if (parent != null) {
            logger.debug("creating receive port for my parent");
            
            String name = parentRportName(me, poolName, instanceCount);
            parentRport = ibis.createReceivePort(PORT_TYPE_ACK, name);
            parentRport.enableConnections();
        }

        if (!children.isEmpty()) {
            logger.debug("creating receive port for my child(ren)");

            String name = childrenRportName(me, poolName, instanceCount);
            childrenRport = ibis.createReceivePort(PORT_TYPE_ACK_ACK, name);
            childrenRport.enableConnections();
        }

        if (parent != null) {
            logger.debug("connecting to my parent: " + parent);

            String sportName = parentSportName(poolName, instanceCount);
            parentSport = ibis.createSendPort(PORT_TYPE_ACK_ACK, sportName);
            String rportName = childrenRportName(parent, poolName, instanceCount);
            parentSport.connect(parent, rportName);
        }

        if (!children.isEmpty()) {
            String sportName = childrenSportName(poolName, instanceCount);
            childrenSport = ibis.createSendPort(PORT_TYPE_ACK, sportName);

            for (IbisIdentifier child: children) {
                String name = parentRportName(child, poolName, instanceCount);
                logger.debug("connecting to my child: " + child);
                childrenSport.connect(child, name);
            }
        }

        synced = false;

        Integer nextInstanceCount = Integer.valueOf(instanceCount + 1);
        instanceCounterMap.put(pool.getName(), nextInstanceCount);
    }

    public static List<PortType> getPortTypes() {
        List<PortType> result = new LinkedList<PortType>();

        result.add(PORT_TYPE_ACK);
        result.add(PORT_TYPE_ACK_ACK);
        
        return result;
    }
        
    private static String parentSportName(String groupID, int instanceCount) {
        return "binack-parent-snd-{" + groupID + "}-[" + instanceCount + "]";
    }

    private static String parentRportName(IbisIdentifier id, String groupID, 
            int instanceCount) {
        return "binack-parent-rcv-" + id.name() + "-{" + groupID + "}-["
                + instanceCount + "]";
    }

    private static String childrenSportName(String groupID, int instanceCount) {
        return "binack-child-snd-{" + groupID + "}-[" + instanceCount + "]";
    }

    private static String childrenRportName(IbisIdentifier id, String groupID, 
            int instanceCount) {
        return "binack-child-rcv-" + id.name() + "-{" + groupID + "}-["
                + instanceCount + "]";
    }

    private void ensureConnected() {
        if (parent != null) {
            // wait until parent connected
            while (parentRport.connectedTo().length < 1) {
                logger.info("my parent " + parent
                        + " has not connected yet, sleeping 1 sec...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    // ignore
                }
            }
        }

        if (!children.isEmpty()) {
            // wait until all children connected
            while (childrenRport.connectedTo().length < children.size()) {
                logger.info("only " + childrenRport.connectedTo().length
                        + " of " + children.size()
                        + " children have connected, sleep 1 sec...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    // ignore
                }
            }
        }
    }

    public synchronized void acknowledge() throws IOException {
        ensureConnected();

        // receive vote from children
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                if (logger.isDebugEnabled()) {
                    logger.debug("receiving ack " + (i + 1) + "-"
                            + children.size());
                }

                ReadMessage fromChild = childrenRport.receive();
                String childName = fromChild.origin().name();
                fromChild.readByte();
                fromChild.finish();
                
                logger.debug("received ack from " + childName);
            }
        }

        // send combined vote to parent and receive result
        if (parent != null) {
            logger.debug("sending ack to parent " + parent);

            WriteMessage toParent = parentSport.newMessage();
            toParent.writeByte((byte)0);
            toParent.finish();

            logger.debug("receiving ack-ack from parent " + parent);

            ReadMessage fromParent = parentRport.receive();
            fromParent.readByte();
            fromParent.finish();
        }

        // send result vote to children
        if (!children.isEmpty()) {
            logger.debug("sending ack-ack to children " + children);

            WriteMessage toChildren = childrenSport.newMessage();
            toChildren.writeByte((byte)0);
            toChildren.finish();
        }

        synced = true;
    }

    public synchronized void close() throws IOException {
        if (!synced) {
            logger.debug("forcing sync before close");
            acknowledge();
        }

        if (parentSport != null)
            parentSport.close();
        if (childrenSport != null)
            childrenSport.close();
        if (parentRport != null)
            parentRport.close();
        if (childrenRport != null)
            childrenRport.close();
    }

}
