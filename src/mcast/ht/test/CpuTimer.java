package mcast.ht.test;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class CpuTimer {

    private Logger logger = Logger.getLogger(CpuTimer.class);
    
    private HashMap<Long, Long> threadCpuTime;
    private HashMap<Long, Long> prevThreadCpuTime;
    private ThreadMXBean mx;
    private int processorCount;
    private boolean measuring;
    
    public CpuTimer() {
        threadCpuTime = new HashMap<Long, Long>();
        prevThreadCpuTime = new HashMap<Long, Long>();

        mx = ManagementFactory.getThreadMXBean();
        
        if (!mx.isThreadCpuTimeSupported()) {
            throw new UnsupportedOperationException("Monitoring thread CPU " +
                    "time is noy supported by this JVM");
        }
        
        mx.setThreadCpuTimeEnabled(true);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        processorCount = os.getAvailableProcessors();
        
        measuring = false;
    }
    
    public synchronized void start() {
        if (measuring) {
            return;
        }
        
        threadCpuTime.clear();
        
        for (long threadId: mx.getAllThreadIds()) {
            long cpuTime = mx.getThreadCpuTime(threadId);
            prevThreadCpuTime.put(threadId, cpuTime);
        }
        
        measuring = true;
    }
    
    public synchronized void stop() {
        if (!measuring) {
            return;
        }
        
        long[] threadIds = mx.getAllThreadIds();
        
        if (prevThreadCpuTime.size() > threadIds.length) {
            logger.warn("Some threads seem to have died " + 
                    "since the last CPU time snapshot (prev: " + 
                    prevThreadCpuTime.size() + ", now: " + threadIds.length +  
                    ")");
        }

        // take a snapshot of all Thread CPU times. Subtract previous CPU times.
        for (long threadId: threadIds) {
            long cpuTime = mx.getThreadCpuTime(threadId);
            
            Long prevCpuTime = prevThreadCpuTime.get(threadId);
            
            if (prevCpuTime != null) {
                // subtract previous CPU time
                long corrected = cpuTime - prevCpuTime.longValue();
                threadCpuTime.put(threadId, corrected);
            } else {
                // new thread
                threadCpuTime.put(threadId, cpuTime);
            }
            
            // replace previous thread CPU time 
            prevThreadCpuTime.put(threadId, cpuTime);
        }
        
        measuring = false;
    }
    
    public synchronized long getTotalCpuTime() {
        if (measuring) {
            throw new IllegalStateException("Please call stop() " +
                    "before retrieving the total CPU time");
        }
        
        long result = 0;

        for (long cpuTime: threadCpuTime.values()) {
            result += cpuTime;
        }
        
        return result;
    }
    
    public int getProcessorCount() {
        return processorCount;
    }
    
}
