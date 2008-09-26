package mcast.p2p;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Set;

import mcast.p2p.storage.Storage;

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
     *                the storage that contains the distributed data
     * @param roots
     *                the set of root ibises that have a complete storage
     *                available
     * @throws IOException
     * @throws IbisException
     */
    public void multicastStorage(Storage storage, Set<IbisIdentifier> roots)
    throws IOException;

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
     * @throws IbisException
     */
    public void close() throws IOException;

}
