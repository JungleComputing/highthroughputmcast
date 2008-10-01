package mcast.p2p;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.Location;

public class DummyIbisIdentifier implements IbisIdentifier {

    private static final long serialVersionUID = 1L;
    private String poolName;
    private Location location;
    
    public DummyIbisIdentifier(String poolName, Location location) {
        this.poolName = poolName;
        this.location = location;
    }
    
    @Override
    public Location location() {
        return location;
    }

    @Override
    public String name() {
        return poolName + "-" + location.getLevel(0);
    }

    @Override
    public String poolName() {
        return poolName;
    }

    @Override
    public int compareTo(IbisIdentifier o) {
        return poolName.compareTo(o.poolName());
    }
    
    @Override
    public String toString() {
        return location.toString();
    }

}
