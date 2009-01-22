package mcast.ht.apps.filecopy;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import mcast.ht.util.Convert;

public class CreateFile {

    private static void usage() {
        System.out.println("usage: java CreateFile <filename> <size>");
        System.exit(1);
    }
    
    public static boolean createFile(File file, long bytes, boolean force) 
    throws IOException
    {
        if (force && file.exists()) {
            if (!file.delete()) {
                throw new IOException("Unable to delete '" + file + "'");
            }
        }
        
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        
        if (file.createNewFile()) {
            if (bytes > 0) {
                // create a file
                RandomAccessFile ra = new RandomAccessFile(file, "rw");
                ra.seek(bytes - 1);
                ra.writeByte(0);
                ra.close();
            }
            return true;
        } else {
            return false;
        }
    }
    
    public static void main(String... args) {
        if (args.length != 2) {
            usage();
        }
        
        File file = new File(args[0]);
        long bytes = Math.round(Convert.parseBytes(args[1]));
        
        if (bytes < 0) {
            System.err.println("Illegal number of bytes: " + bytes);
            System.exit(3);
        }
        
        try {
            if (bytes > 0) {
                System.out.println("Creating file '" + file + "' of " + bytes + " bytes...");
            } else { 
                System.out.println("Creating empty file '" + file + "'...");
            }
            
            if (!createFile(file, bytes, false)) {
                System.out.println("Error: file '" + file + "' already exists");
            } else {
                System.out.println("Done");
            }
        } catch (IOException e) {
            System.err.println("EEK, I/O problems!");
            e.printStackTrace();
        }
    }
    
}
