package mcast.ht;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Set;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.storage.Storage;

public interface MulticastChannel {

    /**
     * Sends all the data in storage to all members of this multicast channel.
     * Initially, only the members in the 'roots' set have a completely filled
     * storage, the storage of all other nodes is empty. This method returns as
     * soon as this node has received all data, and all pieces has been written
     * to the given storage. Since this node could still be relaying data to
     * other members, the storage can only be read, <i>not</i> written. To
     * ensure the storage is writable again, the flush() method must be called.
     * The flush() method will be called automatically if this method is called
     * twice in a row without any flushes in between.
     * 
     * @param storage
     *                the storage that contains the data to distribute
     * @param roots
     *                the set of root ibises that have a complete storage
     *                available. This set cannot be empty.
     * @throws IOException
     */
    public void multicastStorage(Storage storage, Set<IbisIdentifier> roots)
    throws IOException;

    /**
     * Sends all the data in storage to all members of this multicast channel.
     * Which pieces this node already has is indicated in the 'possession' set.
     * Optionally, a set of root nodes can be provided that are known to have
     * all data (e.g. a completely filled storage). This set is used for small 
     * optimizations (e.g. not doing work stealing each cluster that is known 
     * to contain a root node). To guarantee termination, there should be at 
     * least one collective in which all nodes together possess all pieces. 
     * It is up to the application to ensure this is the case.
     * 
     * @param storage
     *                the storage that contains the data to distribute
     * @param roots
     *                an optional set of root ibises that have a complete storage
     *                available. This set can be empty or null if no root nodes
     *                are known.
     * @param possession
     *                a set that contains the indices of all the complete pieces 
     *                in the given storage
     *                
     * @throws IOException
     */
    public void multicastStorage(Storage storage, Set<IbisIdentifier> roots, 
            PieceIndexSet possession) throws IOException;
    
    /**
     * Flushes the last multicast operation. After this method returned, all
     * members of this channel have received all data, and the distributed
     * storage will have been closed.
     */
    public void flush() throws IOException;
    
    /**
     * Logs statistics about the last multicast operation.
     * 
     * @throws IOException
     */
    public void printStats() throws IOException;

    /**
     * Closes this multicast channel. After a channel is closed, it can no 
     * longer be used. If a multicast operation is pending, it is flushed 
     * before closing this channel.
     * 
     * @throws IOException
     */
    public void close() throws IOException;

}
