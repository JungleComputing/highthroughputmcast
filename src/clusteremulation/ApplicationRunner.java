package clusteremulation;

import java.io.File;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;

public class ApplicationRunner {

    private static Logger logger = Logger.getLogger(ApplicationRunner.class);

    private static PoolInfoClient pool;
    private static boolean meHub;
    private static EmulationScript emulationScript;
    private static ClusterEmulation emulation;

    private ApplicationRunner() {
        // prevent construction.
    }
    
    private static void usage() {
        System.err.println("Usage: java clusteremulation.ApplicationRunner "
                + "[ -tell-start | -[no-]tc ]* <emulationScript> <main class> ...");
    }
    
    public static void main(String[] args) {
        
        boolean tellStart = false;
        boolean trafficShaping = true;
       
        logger.info("Examining options ...");
        
        int argc = 0;
        
        for (;;) {        
            if (argc + 2 >= args.length) {
                usage();
                System.exit(1);
            }
            if (args[argc].equals("-tell-start")) {
                tellStart = true;
                argc++;
            } else if (args[argc].equals("-tc")) {
                trafficShaping = true;
                argc++;
            } else if (args[argc].equals("-no-tc")) {
                trafficShaping = false;
                argc++;
            } else if (args[argc].startsWith("-")) {
                usage();
                System.exit(1);                
            } else {
                break;
            }
        }
        
        String emulationFile = args[argc];
        String className = args[argc+1];
        
        // Create arguments array.
        String[] applicationArgs = new String[args.length - 2 - argc];
        for (int i = 0; i < applicationArgs.length; i++) {
            applicationArgs[i] = args[i + 2 + argc];
        }

        logger.info("Creating pool ...");
        pool = PoolInfoClient.create();

        try {
            logger.info("Reading emulation script " + emulationFile);
            emulationScript = new EmulationScript(new File(emulationFile));

            logger.info("Creating cluster emulation");
            emulation = new ClusterEmulation(pool, emulationScript);
        } catch(Throwable e) {
            logger.error("Got exception", e);
            System.exit(1);
        }
        
        meHub = emulation.meHub();

        if (trafficShaping) {
            logger.info("Starting emulation script");
            Thread t = new Thread(emulationScript, "EmulationScript");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();

            if (tellStart) {
                emulationScript.tell("start");
            }
        }

        if (meHub) {
            logger.info("I'm a hub node for the cluster emulation");
        } else {    
            logger.info("I'm an application node in cluster "
                    + emulationScript.getClusterNames()[pool.rank()]);

            // Set properties
            System.setProperty("ibis.pool.size", "" + emulationScript.getHostCount());
            System.setProperty("ibis.location", "node" + pool.rank() + "@"
                    + emulationScript.getClusterNames()[pool.rank()]);
     
            logger.info("examining class to run ...");
    
            // load the class.
            Class<?> cl = null;
            try {
                cl = Class.forName(className);
            } catch (ClassNotFoundException e) {
                try {
                    cl = Thread.currentThread().getContextClassLoader()
                            .loadClass(className);
                } catch(ClassNotFoundException e2) {
                    System.out.println("Could not load class " + className);
                    System.exit(1);
                }
            }
    
            // Find the "main" method.
            Method applicationMain = null;
            try {
                applicationMain = cl.getMethod("main", new Class[] { args.getClass() });
            } catch (Exception e) {
                System.out.println("Could not find a main(String[]) in class "
                        + className);
                System.exit(1);
            }
            
            // Make method accessible, so that it may be called.
            // Note that the main method is public, but the class
            // it lives in may not be.
            if (!applicationMain.isAccessible()) {
                final Method temporary_method = applicationMain;
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        temporary_method.setAccessible(true);
                        return null;
                    }
                });
            }
            
            // Invoke
            try {
                applicationMain.invoke(null, new Object[] { applicationArgs });
            } catch (Throwable e) {
                System.out.println("Got exception in main: " + e);
                e.printStackTrace();
            }
        }
        
        // Barrier for everyone, also hubs
        logger.info("Entering barrier ...");
        System.setProperty("ibis.pool.size", "" + pool.size());
        System.setProperty("ibis.pool.name", "barrier_" + System.getProperty("ibis.pool.name"));
        pool = PoolInfoClient.create();
        logger.info("Exiting barrier ...");
        // Exit explicitly, hubs are not daemon threads.
        System.exit(0);
    }
}
