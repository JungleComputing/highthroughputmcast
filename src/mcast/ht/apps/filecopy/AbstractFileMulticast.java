package mcast.ht.apps.filecopy;


import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import mcast.ht.ConfigProperties;
import mcast.ht.MulticastChannel;
import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.storage.CompositeStorage;
import mcast.ht.storage.IntegerStorage;
import mcast.ht.storage.MemoryMappedFileStorage;
import mcast.ht.storage.RandomAccessFileStorage;
import mcast.ht.storage.Storage;
import mcast.ht.util.Convert;

import org.apache.log4j.Logger;

public abstract class AbstractFileMulticast extends FileMulticast {

    private static final int PIECE_SIZE;
    static {
        ConfigProperties prop = ConfigProperties.getInstance();
        String prop_piece_size = "mcast.ht.apps.filecopy.piece_size";
        String value = prop.getStringProperty(prop_piece_size, "32KB");
        PIECE_SIZE = (int)Convert.parseBytes(value);
    }

    /**
     * Whether to use memory-mapping for files 
     */
    private static final boolean USE_MMAP = false;
    
    /**
     * Minimum file size required to use memory-mapped I/O instead of a 
     * random access file
     */
    private static final int MIN_MMAP_SIZE = 10 * 1024 * 1024;  // bytes

    /**
     * Maximum file size for which memory-mapped I/O is used instead of a 
     * random access file
     */
    private static final long MAX_MMAP_SIZE = (long)(Runtime.getRuntime().maxMemory() * 0.75);

    
    private static final IbisCapabilities REQ_CAPABILITIES = 
        new IbisCapabilities(IbisCapabilities.CLOSED_WORLD,
                IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED,
                IbisCapabilities.ELECTIONS_STRICT);
    
    private static final Logger logger = Logger.getLogger(AbstractFileMulticast.class);
    
    
    private Ibis ibis;
    private MulticastChannel channel;
    
    public AbstractFileMulticast(boolean deleteOnExit) throws Exception {
        super(deleteOnExit);
    }

    public void connect() throws Exception {
        PortType[] portTypes = getPortTypes();
        
        logger.info("Creating Ibis...");
        ibis = IbisFactory.createIbis(REQ_CAPABILITIES, null, portTypes);

        logger.info("Creating multicast channel...");
        channel = createMulticastChannel(ibis);
    }
    
    protected abstract PortType[] getPortTypes();
    
    protected abstract MulticastChannel createMulticastChannel(Ibis ibis)
    throws Exception;

    public void end() throws Exception {
        channel.printStats("");
        channel.close();
        ibis.end();
    }

    public abstract String getImplementationName();

    public void send(FileSet source, File target) throws Exception {
        deleteOnExit(source);
        
        // XXX assumption here: a sender has all files
        // XXX using the target for a local copy on the sender is NOT
        // implemented yet
        Set<IbisIdentifier> roots = Collections.singleton(ibis.identifier());
        
        // 1. multicast the total size of the meta-data
        // XXX assumption: the meta-data's encoded size is never larger than
        // Integer.MAX_VALUE (2GB - 1 byte)
        FileSetStorage metaData = new FileSetStorage(source, PIECE_SIZE);
        int metaDataSize = metaData.getByteSize();

        logger.info("1. Sending meta data size (" + metaDataSize + ")");
        {
            IntegerStorage metaDataSizeStorage = new IntegerStorage(metaDataSize);
            PieceIndexSet one = PieceIndexSetFactory.createFullPieceIndexSet(1);
            channel.multicastStorage(metaDataSizeStorage, roots, one);
        }

        // 2. multicast the meta-data
        logger.info("2. Sending meta data");
        {
            int metaPieceCount = metaData.getPieceCount();
            PieceIndexSet allMetaData = 
                PieceIndexSetFactory.createFullPieceIndexSet(metaPieceCount);
            channel.multicastStorage(metaData, roots, allMetaData);
        }

        // 3. multicast the actual files
        logger.info("3. Sending files");
        {
            CompositeStorage fileData = createStorage(source, true);
            int filePieces = fileData.getPieceCount();
            PieceIndexSet allPieces = 
                PieceIndexSetFactory.createFullPieceIndexSet(filePieces);
            channel.multicastStorage(fileData, roots, allPieces);
            channel.flush();
        }
    }

    public void receive(File target) throws Exception {
        PieceIndexSet nothing = PieceIndexSetFactory.createEmptyPieceIndexSet();

        // 1. receive the total size of the meta-data
        logger.info("1. Receiving meta data size");
        IntegerStorage metaDataSizeStorage = new IntegerStorage(0);
        channel.multicastStorage(metaDataSizeStorage, null, nothing);

        int metaDataSize = metaDataSizeStorage.getValue();

        // 2. receive the meta-data
        logger.info("2. Receiving meta data");
        FileSetStorage metaData = new FileSetStorage(metaDataSize, PIECE_SIZE);
        channel.multicastStorage(metaData, null, nothing);

        FileSet fileSet = metaData.getFileSet(target);
        deleteOnExit(fileSet);
        
        logger.info("3. Receiving files");
        // 3. multicast the actual files
        CompositeStorage fileData = createStorage(fileSet, false);
        channel.multicastStorage(fileData, null, nothing);
        channel.flush();
    }

    private CompositeStorage createStorage(FileSet fileSet, boolean readOnly)
    throws IOException
    {
        CompositeStorage result = new CompositeStorage();

        for (FileInfo fileInfo : fileSet) {
            Storage storage = createStorage(fileInfo, readOnly);
            result.addStorage(storage);
        }

        return result;
    }

    private Storage createStorage(FileInfo fileInfo, boolean readOnly)
    throws IOException
    {
        if (!USE_MMAP ||
            fileInfo.length < MIN_MMAP_SIZE || 
            fileInfo.length > MAX_MMAP_SIZE) {
            // For very small files, memory-mapped I/O is overkill
            // For files > 2GB, we cannot map the whole file into memory
            // In those cases, we use a regular random access file
            logger.info(fileInfo.file.getAbsolutePath() + ": random access");
            return new RandomAccessFileStorage(fileInfo.file, fileInfo.length,
                    PIECE_SIZE, readOnly);
        } else {
            // Use memory-mapped I/O for better write performance
            logger.info(fileInfo.file.getAbsolutePath() + ": mmap");
            return new MemoryMappedFileStorage(fileInfo.file,
                    (int)fileInfo.length, PIECE_SIZE, readOnly);
        }
    }

}
