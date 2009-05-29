package mcast.ht.bittorrent;

/**
 * @author mathijs
 */
public class RateEstimate {

    /**
     * Maximum amount of nanoseconds to estimate the bandwidth over
     */
    private long maxPeriodNanos;    

    /**
     * Start time of the current estimate
     */
    private long rateSince;

    /**
     * The current bandwidth estimate, in bytes per seconds
     */
    private double rate;

    /**
     * Time of the last update
     */
    private long lastUpdate;        

    public RateEstimate(long maxPeriodMillis) {
        maxPeriodNanos = maxPeriodMillis * 1000000L;

        long now = System.nanoTime();

        rateSince = lastUpdate = now;
        rate = 0.0;
    }

    public void updateRate(long amount) {
        if (amount == 0) {
            return;
        }

        long now = System.nanoTime();

        rate = ((rate * (lastUpdate - rateSince)) + amount) / (now - rateSince);

        lastUpdate = now;
        rateSince = Math.max(rateSince, now - maxPeriodNanos);
    }

    public double getRatePerNanosec() {
        return rate;
    }
    
    public void setRatePerNanosec(double rate) {
        this.rate = rate;
    }

}
