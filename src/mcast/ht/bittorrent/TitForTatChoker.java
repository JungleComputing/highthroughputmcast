package mcast.ht.bittorrent;

import ibis.util.ThreadPool;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.TimerTask;

import mcast.ht.net.P2PConnectionNegotiator;
import mcast.ht.util.WeightedSet;

import org.apache.log4j.Logger;

public class TitForTatChoker extends TimerTask
implements Config, InterestListener
{

    private static final Logger logger = Logger
    .getLogger(TitForTatChoker.class);

    private BitTorrentAdmin admin;
    private P2PConnectionNegotiator<? extends BitTorrentConnection> negotiator;
    private Random random;
    private volatile boolean firstTime;
    private volatile boolean ended;

    public TitForTatChoker(BitTorrentAdmin a, 
            P2PConnectionNegotiator<? extends BitTorrentConnection> n) {
        this.admin = a;
        this.negotiator = n;

        random = new Random();
        firstTime = true;
        ended = false;
    }

    public synchronized void run() {
        if (!ended) {
            logger.debug("choking - start");

            LinkedList<BitTorrentConnection> interestedConnections = getInterestedConnections();
            logger.debug("choose from " + interestedConnections.size()
                    + " interested peers");

            if (admin.areAllPieceReceived()) {
                // choke as seed; sort connections ascending by observed upload
                // bandwidth
                logger.debug("choking as seed");
                Collections.sort(interestedConnections,
                        new UploadRateComparator());
                printRates(interestedConnections);
                doChoking(interestedConnections);
            } else {
                // choke as downloader; sort connections ascending by observed
                // download bandwidth
                logger.debug("choking as downloader");
                Collections.sort(interestedConnections,
                        new DownloadRateComparator());
                printRates(interestedConnections);
                doChoking(interestedConnections);
            }

            firstTime = false;

            logger.debug("choking - done");
        }
    }

    public void interested(BitTorrentConnection c) {
        if (!c.amChoked()) {
            logger.debug(c.getPeer() + " became interested, rechoke");

            ThreadPool.createNew(this, "Rechoke");
        }
    }

    public void notInterested(BitTorrentConnection c) {
        if (!c.amChoked()) {
            logger.debug(c.getPeer() + " is not interested anymore, rechoke");

            ThreadPool.createNew(this, "Rechoke");
        }
    }

    public synchronized void end() {
        logger.debug("choking - end");

        ended = true;

        cancel();
    }

    private void printRates(List<BitTorrentConnection> sortedConnections) {
        ListIterator<BitTorrentConnection> listIt = sortedConnections
        .listIterator(sortedConnections.size());

        while (listIt.hasPrevious()) {
            BitTorrentConnection c = listIt.previous();

            logger.debug(c.getPeer() + ": " + c.getRateStats());
        }
    }

    private void doChoking(List<BitTorrentConnection> sortedConnections) {
        ListIterator<BitTorrentConnection> listIt = sortedConnections
        .listIterator(sortedConnections.size());

        // unchoke the TIT_FOR_TAT_PEERS selected peers with the highest
        // download rate
        for (int i = 0; i < TIT_FOR_TAT_PEERS && listIt.hasPrevious(); i++) {
            BitTorrentConnection c = listIt.previous();

            if (c.peerChoked()) {
                logger.debug("unchoking new tit-for-tat peer:  " + c.getPeer());
                c.unchoke();
            } else {
                logger
                .debug("unchoking still tit-for-tat peer: " + c.getPeer());
            }
        }

        // select OPTIMISTIC_UNCHOKE peers for an unchoke;
        // peers without any pieces are 3 times more likely to be selected

        WeightedSet<BitTorrentConnection> set = 
            new WeightedSet<BitTorrentConnection>();

        while (listIt.hasPrevious()) {
            BitTorrentConnection c = listIt.previous();

            if (c.peerHasPieces()) {
                set.add(c, 1);
            } else {
                set.add(c, 3);
            }
        }

        for (int i = 0; i < OPTIMISTIC_UNCHOKE_PEERS; i++) {
            BitTorrentConnection c = (BitTorrentConnection) set.remove(random);

            if (c == null) {
                break;
            } else if (c.peerChoked()) {
                logger.debug("optimistically unchoking " + c.getPeer());
                c.unchoke();
            } else {
                logger.debug("optimistically unchoking " + c.getPeer()
                        + " who was already unchoked");
            }
        }

        // choke all remaining peers
        for (Iterator<BitTorrentConnection> it = set.iterator(); 
                it.hasNext(); ) {
            BitTorrentConnection c = it.next();

            if (!c.peerChoked()) {
                logger.debug("choking " + c.getPeer());
                c.choke();
            }
        }
    }

    private LinkedList<BitTorrentConnection> getInterestedConnections() {
        LinkedList<BitTorrentConnection> list = 
            new LinkedList<BitTorrentConnection>();

        for (BitTorrentConnection c : negotiator) {
            if (c.peerInterested() || firstTime) {
                list.add(c);
            }
        }

        return list;
    }

    static int compareRate(double rate1, double rate2) {
        if (rate1 < rate2) {
            return -1;
        } else if (rate1 > rate2) {
            return 1;
        } else {
            return 0;
        }
    }

    private class DownloadRateComparator
    implements Comparator<BitTorrentConnection> {
        
        public int compare(BitTorrentConnection c1, BitTorrentConnection c2) {
            return compareRate(c1.getDownloadBytesPerNanosec(), c2
                    .getDownloadBytesPerNanosec());
        }

    }

    private class UploadRateComparator
    implements Comparator<BitTorrentConnection> {

        public int compare(BitTorrentConnection c1, BitTorrentConnection c2) {
            return compareRate(c1.getUploadBytesPerNanosec(), c2
                    .getUploadBytesPerNanosec());
        }

    }

}
