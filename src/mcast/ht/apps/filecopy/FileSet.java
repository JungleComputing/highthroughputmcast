package mcast.ht.apps.filecopy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class FileSet implements Iterable<FileInfo> {

    private long KILOBYTE = 1024;
    private long MEGABYTE = KILOBYTE*1024;
    private long GIGABYTE = MEGABYTE*1024;
    
    private LinkedHashSet<FileInfo> files = new LinkedHashSet<FileInfo>();    
    private long dataSize = 0;
    
    public boolean add(FileInfo f) {
        if (files.add(f)) { 
            dataSize += f.file.length();
            return true;
        }
   
        return false;     
    }
        
    public boolean add(File f) {
        try { 
            return add(f, true, false, false);
        } catch (IOException e) {
            return false;
        }
    }
    
    public long dataSize() { 
        return dataSize;
    }
    
    public int size() { 
        return files.size();
    }

    public String humanReadableSize() {
        
        if (dataSize > GIGABYTE) {
            return ((10 * dataSize / GIGABYTE) / 10.0) + " GB";
        }

        if (dataSize > MEGABYTE) {
            return ((10 * dataSize / MEGABYTE) / 10.0) + " MB";
        }

        return ((10 * dataSize / KILOBYTE) / 10.0) + " KB";
    }

    public boolean add(String name, boolean recursive, boolean force, boolean silent) throws IOException {
    
        File f = new File(name);

        // we want to copy the file or directory without creating all its parent
        // directories in the target nodes, so we have to chop of this 'prefix'
        String prefix = f.getParent();
        	
    	if (prefix == null) {
    		prefix = "";
    	}
        
//        if (f.isDirectory() && !name.endsWith(File.separator)) {
//            name = name + File.separator;
//        }
        
        return add(prefix, f, recursive, force, silent);
    }
    
    public boolean add(String prefix, File f, boolean recursive, boolean force, boolean silent) throws IOException {

        System.err.println("adding: " + f.getPath() + " (prefix: " + prefix + ")");
        
        if (!f.exists() && !force) {
            if (silent) { 
                return false;
            }
            throw new FileNotFoundException("File not found: " + f.getName());
        }
        
        if (!f.canRead() && !force) {
            if (silent){ 
                return false;
            }
            throw new IOException("File not readable: " + f.getName());
        }
        
        if (!f.isDirectory()) {           
            return add(new FileInfo(f, prefix));            
        }
        
        if (recursive) {
            
            boolean result = false;
            
            File [] tmp = f.listFiles();
            
            if (tmp != null) { 
                for (File file : tmp) {
                    result = add(prefix, file, recursive, force, silent) || result;
                }
            }
            
            return result;
        }        
        
        return false;
    
    }
    
    public boolean add(File f, boolean recursive, boolean force, boolean silent) throws IOException {

        System.err.println("adding: " + f.getPath());
        
        if (!f.exists() && !force) {
            if (silent) { 
                return false;
            }
            throw new FileNotFoundException("File not found: " + f.getName());
        }
        
        if (!f.canRead() && !force) {
            if (silent){ 
                return false;
            }
            throw new IOException("File not readable: " + f.getName());
        }
        
        if (!f.isDirectory()) {           
            return add(new FileInfo(f, f.getPath()));            
        }
        
        if (recursive) {
            
            boolean result = false;
            
            File [] tmp = f.listFiles();
            
            if (tmp != null) { 
                for (File file : tmp) {
                    result = add(file, recursive, force, silent) || result;
                }
            }
            
            return result;
        }        
        
        return false;
    }
    
    public Iterator<FileInfo> iterator() {
        return files.iterator();
    }    
}
