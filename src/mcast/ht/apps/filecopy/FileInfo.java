package mcast.ht.apps.filecopy;

import java.io.File;

public class FileInfo {

    public final File file;
    public final String prefix;
    public final long length;

    public FileInfo(File file, String prefix) {
        this.file = file;
        this.prefix = prefix;
        
        length = file.length();
    }
    
    public FileInfo(File file, String prefix, long length) {
        this.file = file;
        this.prefix = prefix;
        this.length = length;
    }

    public int hashCode() { 
        return file.hashCode();
    }
    
    public boolean equals(Object other) { 
        return other instanceof FileInfo && 
            file.equals(((FileInfo)other).file);
    }

    public String getPath() {
        
        String path = file.getPath();
        
       // System.err.println("Prefix: " + prefix);
        
        if (path.length() > prefix.length() && path.startsWith(prefix)) { 
            return path.substring(prefix.length());
        }
        
        return path;
    }
}