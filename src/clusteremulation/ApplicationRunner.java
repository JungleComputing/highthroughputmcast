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
    
    public static void main(String[] args) {
       
        logger.info("Examining options ...");
        
        if (args.length < 2) {
            System.err.println("Usage: java "
                    + " clusteremulation.ApplicationRunner <emulationScript> <main class> ...");
            System.exit(1);
        }
        
        String emulationFile = args[0];
        String className = args[1];
        
        // Create arguments array.
        String[] applicationArgs = new String[args.length - 2];
        for (int i = 0; i < applicationArgs.length; i++) {
            applicationArgs[i] = args[i + 2];
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

        /*
        logger.info("Starting emulation script");
        Thread t = new Thread(emulationScript, "EmulationScript");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        */

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
            } catch (Exception e) {
                System.out.println("Could not invoke main: " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        // Barrier for everyone, also hubs
        System.setProperty("ibis.pool.size", "" + pool.size());
        System.setProperty("ibis.pool.name", "barrier_" + System.getProperty("ibis.pool.name"));
        pool = PoolInfoClient.create();
    }
}