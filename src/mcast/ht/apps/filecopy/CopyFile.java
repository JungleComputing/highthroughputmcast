
package mcast.ht.apps.filecopy;

import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import mcast.ht.util.Convert;

public class CopyFile {

    private static boolean verbose = false;  
    private static boolean force = true;  

    private FileMulticast mcast; 

    public CopyFile(String impl, boolean deleteOnExit) throws Exception {        
        mcast = FileMulticast.create(impl, deleteOnExit);
        
        mcast.connect();     

        if (verbose) { 
            System.out.println("FileCopy 0.1");
            System.out.println("Using implementation: " + mcast.getImplementationName());
        }
    }

    public void printPerformance(PrintStream out, String header, double time, 
            long size) { 

        double tp = 0;

        String tunit = "msec.";
        String tpunit = "MByte/s";

        if (size > 0) { 
            tp = (size / (1024.0*1024.0)) / ((time)/1000.0);

            if (tp > 1000) { 
                tp = (tp / 1024.0);
                tpunit = "GByte/s";
            }                
        }

        if (time > 1000) { 
            time = time/1000.0;
            tunit = "sec.";
        }
        
        out.printf("%s %.1f %s (%3.1f %s)\n", header, time, tunit, tp, tpunit);
    }

    private void copy(FileSet sources, File target, boolean sender) { 
        try { 
            if (sender) {
                long start = System.currentTimeMillis();

                mcast.send(sources, target);

                long end = System.currentTimeMillis();

                //if (verbose) {
                    printPerformance(System.out, "Total time", end-start, 
                            sources.dataSize());
                //}                                 
            } else { 
                mcast.receive(target);
            }

            mcast.end();            
        } catch (Exception e) { 
            System.err.println("Error while copying files!");
            e.printStackTrace();
        }
    }

    private static String getHostname() {         
        try { 
            InetAddress a = InetAddress.getLocalHost();

            if (a != null) {
                return a.getHostName();
            } else { 
                System.out.println("Failed to get local address");
                System.exit(1);
            }
        } catch (Exception e) {
            System.out.println("Failed to get local address");
            System.exit(1);
        }        
        return null;
    }

    public static void main(String[] args) {
        try { 

            /* 
             * Expects the following parameters: 
             *
             * -sender <hostname>        hostname of a sender machine    
             * -s <file or directory>    source file or directory
             * -t <file or directory>    target file or directory
             * -create <size>            create a file of the given size 
             *                           on each source node
             * -deleteOnExit             delete created and copied files 
             *                           on program exit
             */

            String impl = "mcast.ht.apps.filecopy.RobberFileMulticast";
            List<String> senderHosts = new LinkedList<String>();
            LinkedList<String> sourceFiles = new LinkedList<String>();
            String targetDirectory = null;        
            long createBytes = -1;
            boolean deleteOnExit = false;

            for (int i=0; i<args.length; i++) {             
                if (args[i].equals("-sender")) { 
                    senderHosts.add(args[++i]);
                } else if (args[i].equals("-sourceFile") || args[i].equals("-s")) {
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        sourceFiles.addLast(args[++i]);
                    }
                } else if (args[i].equals("-targetDirectory") || args[i].equals("-t")) { 
                    targetDirectory = args[++i];
                } else if (args[i].equals("-create")) { 
                    createBytes = Math.round(Convert.parseBytes(args[++i]));            
                } else if (args[i].equals("-deleteOnExit")) { 
                    deleteOnExit = true;            
                } else if (args[i].equals("-verbose") || args[i].equals("-v")) { 
                    verbose = true;       
                } else if (args[i].equals("-impl")) {
                    impl = args[++i];
                } else { 
                    System.err.println("Unknown option: " + args[i]);
                }
            }

            String local = getHostname();       

            boolean sender = false;

            if (senderHosts.isEmpty()) { 
                System.err.println("Missing sender(s)!");
                System.exit(1);
            } else {
                for (String host: senderHosts) {
                    if (local.startsWith(host)) {
                        sender = true;
                    }
                }
            }

            File target = null;
            FileSet sources = null;

            if (sender) { 
                if (createBytes >= 0) {
                    if (sourceFiles.size() > 1) {
                        System.err.println("Cannot create more than one file");
                        System.exit(1);
                    }
                
                    // create the source file
                    String name = sourceFiles.iterator().next();
                    System.out.println("Creating file '" + name + "' of " + 
                            createBytes + " bytes...");
                    
                    File f = new File(name);
                     
                    if (deleteOnExit) {
                        f.deleteOnExit();
                    }
                    
                    if (!CreateFile.createFile(f, createBytes, force)) {
                        System.err.println("File '" + f + "' already exists!");
                        System.exit(1);
                    }
                }

                sources = new FileSet();

                for (String f : sourceFiles) { 
                    sources.add(f, true, force, true);
                }
            }

            if (!sender) { 
                if (targetDirectory == null) { 
                    System.err.println("No target directory");
                    System.exit(1);
                }

                target = new File(targetDirectory);

                if (deleteOnExit) {
                    target.deleteOnExit();
                }
            }

            CopyFile fc = new CopyFile(impl, deleteOnExit);
            fc.copy(sources, target, sender);

            if (deleteOnExit && target != null && target.exists()) {
                System.out.println("Deleting '" + target + "'...");
                try { 
                    target.delete();
                } catch (Exception e) {
                    System.err.println(local + ": Failed to delete - " 
                            + target.getPath());
                }
            }

        } catch (Exception e) {
            System.err.println("EEK: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
