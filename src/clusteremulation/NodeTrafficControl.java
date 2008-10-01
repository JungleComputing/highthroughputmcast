package clusteremulation;

import java.io.IOException;
import java.net.InetAddress;

import mcast.p2p.util.Convert;

import org.apache.log4j.Logger;

public class NodeTrafficControl extends AbstractTrafficControl {

    private static Logger logger = Logger.getLogger(NodeTrafficControl.class);

    private boolean emulateBandwidth;
    private InetAddress[] hubAddrs;
    private int myRank;
    private double prevOutCap;

    public NodeTrafficControl(EmulatedGauge gauge, boolean emulateBandwidth, 
            InetAddress[] hubAddrs, int myRank) throws IOException {
        super(gauge);

        this.emulateBandwidth = emulateBandwidth;
        this.hubAddrs = hubAddrs;
        this.myRank = myRank;

        prevOutCap = -1;
    }

    /**
     * Sets up local traffic control to emulate a certain outgoing capacity to a
     * number of IP addresses.
     */
    @Override
    protected void updateEmulation(EmulatedGauge gauge, boolean firstTime) 
    throws IOException {
        if (!emulateBandwidth) {
            return;
        }

        double outCap = gauge.getOutgoingHostCapacity(myRank);

        if (outCap <= 0) {
            throw new IOException("cannot emulate the outgoing capacity of host " + 
                    myRank + ": it is set to " + outCap + " bytes/sec");
        }

        if (firstTime || outCap != prevOutCap) {
            logger.info("Emulating outgoing capacity of " + myRank + ": " + 
                    Convert.round(outCap, 2) + " bytes/sec");

            if (firstTime) {
                // add the root HTB qdisc
                execute(true, "sudo", "/sbin/tc", "qdisc", "add", "dev", 
                        "myri0", "root", "handle", "1:", "htb", "default", "2"); 
            }

            // add or change the outgoing cluster capacity
            execute(true, "sudo", "/sbin/tc", "class", 
                    (firstTime ? "add" : "change"), "dev", "myri0", "parent", 
                    "1:", "classid", "1:1", "htb", 
                    "rate", bandwidthValue(outCap), "mtu", mtuValue(), 
                    "burst", burstValue(outCap), "cburst", cburstValue(outCap));

            prevOutCap = outCap;

            if (firstTime) {
                // add rules so only the traffic going to certain addresses is
                // emulated
                for (int i = 0; i < hubAddrs.length; i++) {
                    String ip = hubAddrs[i].getHostAddress();
                    execute(true, "sudo", "/sbin/tc", "filter", "add", "dev", 
                            "myri0", "protocol", "ip", "parent", "1:0", "u32",
                            "match", "ip", "dst", ip, "flowid", "1:1");
                }
            }
        }
    }

}
