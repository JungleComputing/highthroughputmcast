package mcast.p2p.robber;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.admin.PieceIndexSetFactory;
import mcast.p2p.admin.SynchronizedPieceIndexSet;
import mcast.p2p.bittorrent.BitTorrentAdminImpl;
import mcast.p2p.net.P2PConnection;
import mcast.p2p.storage.Piece;
import mcast.p2p.util.Convert;

import org.apache.log4j.Logger;

public class RobberAdminImpl extends BitTorrentAdminImpl
implements RobberAdmin, Config
{

    private Logger logger = Logger.getLogger(RobberAdminImpl.class);

    private final Set<IbisIdentifier> localPeers;
    private final boolean doStealing;
    private final Map<Object, RobberConnection> localConnectionMap;
    private final List<RobberConnection> globalConnections;
    private final Random random;
    private volatile boolean stealingWork;
    private final List<IbisIdentifier> localLabourForce;
    private final PieceIndexSet pendingPieces;

    private int stolenFromMe;
    private int stolenByMe;
    private int stealRequestedCount;
    private int stealReceivedCount;
    private int stealRequestedAndFailedCount;
    private int stealReceivedAndFailedCount;

    public RobberAdminImpl(int totalPieces, PieceIndexSet possession,
            Set<IbisIdentifier> localPeers, PieceIndexSet work, 
            boolean doStealing) {
        super(totalPieces, possession, possession.not(totalPieces), work);

        this.localPeers = localPeers;
        this.doStealing = doStealing;

        localConnectionMap = new HashMap<Object, RobberConnection>();
        globalConnections = new ArrayList<RobberConnection>();

        random = new Random(mcast.p2p.Config.RANDOM_SEED);

        stealingWork = false;
        localLabourForce = Collections.synchronizedList(
                new LinkedList<IbisIdentifier>());

        pendingPieces = new SynchronizedPieceIndexSet(
                PieceIndexSetFactory.createEmptyPieceIndexSet());

        stolenFromMe = 0;
        stolenByMe = 0;
        stealRequestedCount = 0;
        stealReceivedCount = 0;
        stealRequestedAndFailedCount = 0;
        stealReceivedAndFailedCount = 0;
    }

    @Override
    public synchronized void addConnection(P2PConnection c) {
        super.addConnection(c);

        if (localPeers.contains(c.getPeer())) {
            localConnectionMap.put(c.getPeer(), (RobberConnection)c);
        } else {
            globalConnections.add((RobberConnection)c);
        }
    }

    public PieceIndexSet getDesire(IbisIdentifier peer) {
        if (localPeers.contains(peer)) {
            // from local peers we desire both gold and silver pieces
            synchronized(interest) {
                PieceIndexSet gold = interest.getGold();
                PieceIndexSet silver = interest.getSilver();

                return gold.or(silver);
            }
        } else {
            // from global peers, we desire only gold pieces
            return interest.getGold();
        }
    }

    public boolean haveWorkForLocalPeer(IbisIdentifier peer) {
        return STEALING && localPeers.contains(peer) && interest.containsGold();
    }

    public void peerFoundWork(IbisIdentifier peer) {
        synchronized(localLabourForce) {
            if (!localLabourForce.contains(peer)) {
                localLabourForce.add(peer);
            }
        }

        checkWorkAvailable();
    }

    private void checkWorkAvailable() {
        if (!STEALING) {
            return;
        }

        boolean haveGold = interest.containsGold();

        synchronized (localLabourForce) {
            if (!doStealing || haveGold || stealingWork || 
                    globalConnections.isEmpty() || localLabourForce.isEmpty() ||
                    (!haveGold && (pendingPieces.size() + piecesReceived.size() == totalPieces))) {
                /**
                 * We do not have to find new work if: 
                 * - we do not steal in the first place (which is the case in 
                 *   the root cluster) 
                 * - we still have work left (the golden pieces) 
                 * - we are already stealing work 
                 * - there are no connections to global peers (in which we can 
                 *   never participate in stealing certain pieces from global 
                 *   peers)
                 * - we do not know any local peer that might have work 
                 * - all pieces are either pending (being requested) or received
                 */
                return;
            }

            // hmm, I'm not sure why we lock the interest here...
            synchronized (interest) {
                // time to steal new work from our local peers
                stealingWork = true;

                int selected = random.nextInt(localLabourForce.size());
                Object selectedPeer = localLabourForce.get(selected);
                RobberConnection c = localConnectionMap.get(selectedPeer);

                c.stealWork();

                stealRequestedCount++;

                return;
            }
        }
    }

    public void giveWork(IbisIdentifier peer, PieceIndexSet booty) {
        stealingWork = false;

        if (booty.isEmpty()) {
            // peer returned no work
            stealRequestedAndFailedCount++;
            localLabourForce.remove(peer);
            checkWorkAvailable();
            return;
        }

        // revaluate all pieces in the booty in which we are still interested
        PieceIndexSet newGold = null;

        synchronized(interest) {
            interest.revaluate(booty);

            if (logger.isDebugEnabled()) {
                logger.debug("stole " + booty + ", interest=" + interest);
            }

            stolenByMe += booty.size();

            // determine our new desire (everything part of our new work)
            newGold = interest.getGold();
        }

        // The new work sequence:

        // 1. Send our new desire to all our global peers
        for (RobberConnection c: globalConnections) {
            c.sendDesire(newGold);
        }

        // 2. Notify all connections to global peers that we found new work.
        // This initiated piece requests, if we found something we desire.
        // We should notify global peers before local ones to avoid that
        // a global peers steals our work before we had a change to request it.
        for (RobberConnection c: globalConnections) {
            c.receivedWork();
        }

        // 3. Notify all local peers that we found new work
        for (RobberConnection c: localConnectionMap.values()) {
            c.sendFoundWork();
        }

    }

    public PieceIndexSet stealWork(double fraction) {
        synchronized(interest) {
            PieceIndexSet booty = interest.devaluateFirst(fraction);

            if (logger.isDebugEnabled()) {
                logger.debug("got robbed of " + booty + ", interest=" + interest);
            }

            stealReceivedCount++;
            stolenFromMe += booty.size();

            if (booty.isEmpty()) {
                stealReceivedAndFailedCount++;
            } else {
                // notify all global peers about our new desire 
                // (everything still part of our work)
                PieceIndexSet remainingGold = interest.getGold();

                for (RobberConnection c: globalConnections) {
                    c.sendDesire(remainingGold);
                }
            }

            return booty;
        }
    }

    @Override
    public boolean addExistence(Object peer, int pieceIndex) {
        // From global peers, we want only golden pieces. From local peers, we
        // want both golden and silver pieces.
        boolean isLocalPeer = localPeers.contains(peer);

        if (isLocalPeer) {
            // when we know a local peer has a piece, we can remove it from our work;
            // this save us from getting the piece from a global peer
            interest.devaluate(pieceIndex);
        }

        return interest.tellHave(peer, pieceIndex, !isLocalPeer);
    }

    @Override
    public boolean addExistence(Object peer, PieceIndexSet pieceIndices) {
        // From global peers, we want only golden pieces. From local peers, we
        // want both golden and silver pieces.
        boolean isLocalPeer = localPeers.contains(peer);

        if (isLocalPeer) {
            // when we know a local peer has a piece, we can remove it from our work;
            // this save us from getting the piece from a global peer
            interest.devaluate(pieceIndices);
        }

        return interest.tellHave(peer, pieceIndices, !isLocalPeer);
    }

    @Override
    public int[] requestDesiredPieceIndices(Object peer, int amount) {
        // XXX since we do not use the BitTorrent code for the piece picking,
        // we cannot use end game mode in Robber. To do that, the end game
        // code should be separated from the BitTorrentAdminImpl
        int[] result = null;

        if (localPeers.contains(peer)) {
            // we desire both gold and silver pieces from our local peers
            result = interest.removeGoldOrSilver(peer, amount);
        } else {
            // we desire only gold pieces from our global peers
            result = interest.removeGold(peer, amount);
        }

        for (int i: result) {
            pendingPieces.add(i);
        }

        checkWorkAvailable();

        return result;
    }

    @Override
    public void setPieceReceived(IbisIdentifier origin, Piece piece) {
        pendingPieces.remove(piece.getIndex());

        super.setPieceReceived(origin, piece);
    }

    @Override
    public void printStats()
    throws IOException
    {
        super.printStats();

        String stealRequestedFailedPerc = stealRequestedCount == 0 ? "0" : 
            Convert.round(stealRequestedAndFailedCount * 100 / 
                    (double) stealRequestedCount, 2);

        String stealReceivedFailedPerc = stealReceivedCount == 0 ? "0" : 
            Convert.round(stealReceivedAndFailedCount * 100 / 
                    (double) stealReceivedCount, 2);

        Config.statsLogger.info("steal_stats " + stolenByMe + " pieces in " +
                stealRequestedCount + " requests by me (" +
                stealRequestedAndFailedCount + " = " + 
                stealRequestedFailedPerc + "% useless), " + stolenFromMe + 
                " pieces in " + stealReceivedCount + " requests from me (" + 
                stealReceivedAndFailedCount + " = " + stealReceivedFailedPerc + 
                "% useless)");
    }

}
