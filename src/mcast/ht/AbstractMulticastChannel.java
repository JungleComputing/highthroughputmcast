package mcast.ht;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.storage.Storage;
import mcast.ht.util.Defense;

public abstract class AbstractMulticastChannel implements MulticastChannel {

    private Logger logger = Logger.getLogger(AbstractMulticastChannel.class);
    
    protected IbisIdentifier me;
    private boolean flushed;
    private boolean closed;

    public AbstractMulticastChannel(IbisIdentifier me) {
        this.me = me;
        flushed = true;
        closed = false;
    }

    protected Set<IbisIdentifier> joinEverybody(Ibis ibis, 
            Set<IbisIdentifier> ibises) {
        
        IbisIdentifier me = ibis.identifier();
        
        if (ibises.contains(me)) {
            return ibises;
        } else {
            Set<IbisIdentifier> result = new HashSet<IbisIdentifier>(ibises);
            result.add(me);
            return result;
        }
    }
    
    public synchronized void multicastStorage(Storage storage, 
            Set<IbisIdentifier> roots) throws IOException {

        PieceIndexSet myPossession = determinePossession(storage, roots);
        multicastStorage(storage, roots, myPossession);
    }

    public synchronized void multicastStorage(Storage storage,
            Set<IbisIdentifier> roots, PieceIndexSet possession) 
    throws IOException {
        checkNotClosed();

        Defense.checkNotNull(storage, "storage");
        Defense.checkNotNull(roots, "roots");

        if (roots.isEmpty()) {
            throw new IllegalArgumentException("the set of roots cannot be empty");
        }

        if (!flushed) {
            // flush a previous multicast operation
            flush();
        }

        flushed = false;

        long time = 0;
        
        if (logger.isInfoEnabled()) {
            time = System.currentTimeMillis();
        }
        
        doMulticastStorage(storage, roots, possession);
        
        if (logger.isInfoEnabled()) {
            time = System.currentTimeMillis() - time;
            logger.info(me + " found all pieces in " + time + " ms.");
        }
    }

    public synchronized void flush() throws IOException {
        checkNotClosed();

        if (flushed) {
            return;
        }

        doFlush();
        flushed = true;
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("cannot flush a closed " +
            "multicast channel");
        }
    }

    public synchronized void close() throws IOException {
        try {
            doClose();
        } finally {
            closed = true;
        }
    }

    private PieceIndexSet determinePossession(Storage storage, 
            Set<IbisIdentifier> roots) {
        if (roots.contains(me)) {
            // we are one of the root nodes
            int totalPieces = storage.getPieceCount();
            return PieceIndexSetFactory.createFullPieceIndexSet(totalPieces);
        } else {
            // we are not one of the root nodes
            return PieceIndexSetFactory.createEmptyPieceIndexSet();
        }
    }

    protected abstract void doMulticastStorage(Storage storage, 
            Set<IbisIdentifier> roots, PieceIndexSet possession) 
    throws IOException;


    protected abstract void doFlush() throws IOException;

    protected abstract void doClose() throws IOException;

}