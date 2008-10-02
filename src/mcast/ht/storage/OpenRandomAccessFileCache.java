package mcast.ht.storage;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class OpenRandomAccessFileCache implements Config {

    private static Logger logger = 
        Logger.getLogger(OpenRandomAccessFileCache.class);

    private final LRURandomAccessFileHashMap openFiles;

    protected OpenRandomAccessFileCache() {
        openFiles = new LRURandomAccessFileHashMap();
        logger.info("max. open files: " + MAX_OPEN_FILES);
    }

    public static OpenRandomAccessFileCache getInstance() {
        return SingletonHolder.instance;
    }

    public synchronized RandomAccessFile getRandomAccessFile(File file, 
            boolean readOnly) throws IOException {
        RandomAccessFile result = openFiles.get(file);

        if (result == null) {
            result = new RandomAccessFile(file, readOnly ? "r" : "rw");
            openFiles.put(file, result);
        }

        return result;
    }

    public synchronized void closeRandomAccessFile(File file) throws IOException {
        RandomAccessFile raf = openFiles.get(file);

        if (raf != null) {
            synchronized(raf) {
                try {
                    raf.close();
                    openFiles.remove(file);
                } catch (IOException e) {
                    logger.error("error while closing random access to " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    private static class SingletonHolder {
        static OpenRandomAccessFileCache instance = new OpenRandomAccessFileCache();    
    }

    private static class LRURandomAccessFileHashMap extends LinkedHashMap<File, RandomAccessFile> {

        private static final long serialVersionUID = 1138463725330253310L;

        LRURandomAccessFileHashMap() {
            super((int)(4 / 3 * MAX_OPEN_FILES), 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<File, RandomAccessFile> eldest) {
            if (size() > MAX_OPEN_FILES) {
                RandomAccessFile raf = eldest.getValue();
                File file = eldest.getKey(); 

                synchronized(raf) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        logger.error("error while closing random access to " + 
                                file.getAbsolutePath(), e);
                    }
                }

                return  true;
            } else {
                return false;
            }
        }

    }

}
