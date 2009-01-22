package mcast.ht.apps.filecopy;

import java.io.File;
import java.lang.reflect.Constructor;

public abstract class FileMulticast {

    protected boolean deleteOnExit;
    
    public FileMulticast(boolean deleteOnExit) { 
        this.deleteOnExit = deleteOnExit;
    }
   
    public abstract void connect() throws Exception;
    public abstract void send(FileSet source, File target) throws Exception;
    public abstract void receive(File target) throws Exception;
    public abstract void end() throws Exception;
    
    public abstract String getImplementationName();
    
    protected void deleteOnExit(FileSet set) {
        if (deleteOnExit) {
            for (FileInfo f: set) {
                f.file.deleteOnExit();
            }
        }
    }
    
    public static FileMulticast create(String name, boolean deleteOnExit) 
    throws Exception 
    { 
        try { 
            @SuppressWarnings("unchecked")
            Class<FileMulticast> c = (Class<FileMulticast>) Class.forName(name);
        
            Constructor<FileMulticast> constructor = c.getConstructor( 
                    new Class [] { boolean.class });
        
            return constructor.newInstance(new Object [] { deleteOnExit });
        } catch (Exception e) {
            throw new Exception("Failed to load Broadcast type: " + name, e);
        }
    }        
}
