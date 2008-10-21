package mcast.ht.storage;

import java.io.IOException;

public interface VerifiableStorage extends Storage {

    /**
     * Returns a unique value for the contents of this storage
     * (e.g. a MD5 sum or similar hash value). This can be used to verify that 
     * the data in this storage has not been mangled during a multicast
     * operation.  
     * 
     * @return a unique digest value for this storage.
     */
    public byte[] getDigest() throws IOException;
    
    /**
     * Sets all data in this storage to zero.
     */
    public void clear() throws IOException;

}
