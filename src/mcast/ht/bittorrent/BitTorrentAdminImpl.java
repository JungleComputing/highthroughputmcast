package mcast.ht.bittorrent;

import ibis.ipl.IbisIdentifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;
import mcast.ht.admin.PieceInterest;
import mcast.ht.admin.PieceInterestFactory;
import mcast.ht.admin.SynchronizedPieceIndexSet;
import mcast.ht.net.P2PConnection;
import mcast.ht.storage.Piece;

import org.apache.log4j.Logger;

public class BitTorrentAdminImpl implements Config, BitTorrentAdmin {

    private static final Logger logger = Logger.getLogger(BitTorrentAdminImpl.class);

    protected final int totalPieces;
    private final Set<BitTorrentConnection> connections;

    /**
     * Keep track of the pieces in which this host is interested, so we know
     * which pieces to request from which peers
     */
    protected final PieceInterest interest;

    /**
     * Keep track of which pieces have been received, so we know which pieces we
     * can send to our peers and when we have received all pieces ourselves.
     */
    protected final PieceIndexSet piecesReceived;

    /**
     * Per peer, keep track of which pieces are pending (that is: requested but
     * not received yet)
     */
    private final Map<Object, PieceIndexSet> pendingPiecesMap;

    /**
     * Per peer, keep track of which pieces can be requested again. This only
     * happens in end game mode.
     */
    private final Map<Object, PieceIndexSet> endGamePiecesMap;

    /**
     * Are we in end game mode yet?
     */
    private volatile boolean endGame;

    public BitTorrentAdminImpl(int totalPieces, PieceIndexSet possession) {
        this(totalPieces, possession, 
                PieceIndexSetFactory.createEmptyPieceIndexSet(), 
                possession.not(totalPieces));
    }

    protected BitTorrentAdminImpl(int totalPieces, PieceIndexSet possession,
            PieceIndexSet silver, PieceIndexSet gold) {
        this.totalPieces = totalPieces;

        piecesReceived = possession.deepCopy();

        interest = PieceInterestFactory.createPieceInterest(totalPieces,
                silver, gold);

        connections = new HashSet<BitTorrentConnection>();

        if (END_GAME) {
            pendingPiecesMap = Collections
            .synchronizedMap(new HashMap<Object, PieceIndexSet>());
            endGamePiecesMap = new HashMap<Object, PieceIndexSet>();
        } else {
            pendingPiecesMap = null;
            endGamePiecesMap = null;
        }

        endGame = false;
    }

    public synchronized void addConnection(P2PConnection c) {
        connections.add((BitTorrentConnection)c);

        if (END_GAME) {
            pendingPiecesMap.put(c.getPeer(), new SynchronizedPieceIndexSet(
                    PieceIndexSetFactory.createEmptyPieceIndexSet()));
            endGamePiecesMap.put(c.getPeer(), new SynchronizedPieceIndexSet(
                    PieceIndexSetFactory.createEmptyPieceIndexSet()));
        }
    }

    public int getNoTotalPieces() {
        return totalPieces;
    }

    public synchronized boolean isPieceReceived(int index) {
        return piecesReceived.contains(index);
    }

    public boolean addExistence(Object peer, int pieceIndex) {
        return interest.tellHave(peer, pieceIndex, false);
    }

    public boolean addExistence(Object peer, PieceIndexSet pieceIndices) {
        return interest.tellHave(peer, pieceIndices, false);
    }

    public int[] requestDesiredPieceIndices(Object peer, int amount) {
        int[] result = interest.removeGold(peer, amount);

        if (END_GAME) {
            if (result.length > 0) {
                PieceIndexSet pending = pendingPiecesMap.get(peer);

                synchronized (pending) {
                    for (int i : result) {
                        pending.add(i);
                    }
                }
            }

            synchronized (endGamePiecesMap) {
                if (!endGame && !interest.containsGold()) {
                    logger.debug("starting end game");

                    endGame = true;

                    for (BitTorrentConnection c : connections) {
                        Object p = c.getPeer();
                        PieceIndexSet endGamePieces = getEndGamePieces(p);
                        logger.debug("end game pieces for " + p + ": "
                                + endGamePieces);
                        endGamePiecesMap.put(p, endGamePieces);
                    }
                }
            }

            if (endGame) {
                PieceIndexSet endGamePieces = endGamePiecesMap.get(peer);

                synchronized (endGamePieces) {
                    if (!endGamePieces.isEmpty() && result.length < amount) {
                        // add end game pieces

                        int piecesAvailable = result.length
                                + endGamePieces.size();
                        if (piecesAvailable > amount) {
                            piecesAvailable = amount;
                        }

                        int[] newResult = new int[piecesAvailable];

                        // first, copy the pieces we picked before
                        System.arraycopy(result, 0, newResult, 0, 
                                result.length);

                        // second, fill up the new pieces array with pending
                        // pieces
                        Iterator<Integer> optionsIt = endGamePieces.iterator();
                        for (int i = result.length; i < piecesAvailable; i++) {
                            newResult[i] = optionsIt.next();
                            optionsIt.remove();
                        }

                        logger.debug("[end game] returning " + result.length
                                + " normal and "
                                + (piecesAvailable - result.length)
                                + " duplicate pieces out of " + endGamePieces
                                + ": " + Arrays.toString(newResult));

                        result = newResult;
                    }
                }
            }
        }

        return result;
    }

    private PieceIndexSet getEndGamePieces(Object peer) {
        PieceIndexSet result = PieceIndexSetFactory.createEmptyPieceIndexSet();

        for (Map.Entry<Object, PieceIndexSet> entry : pendingPiecesMap.entrySet()) {
            Object h = entry.getKey();

            for (BitTorrentConnection c : connections) {
                if (!c.getPeer().equals(h)) {
                    PieceIndexSet pending = entry.getValue();

                    result.addAll(pending);
                }
            }
        }

        PieceIndexSet alreadyRequested = pendingPiecesMap.get(peer);

        for (int i : alreadyRequested) {
            result.remove(i);
        }

        return result;
    }

    public synchronized PieceIndexSet getPiecesReceived() {
        return piecesReceived.deepCopy();
    }

    public synchronized int getPiecesReceivedCount() {
        return piecesReceived.size();
    }

    public synchronized void setPieceReceived(IbisIdentifier origin, 
            Piece piece) {
        // update the administration
        piecesReceived.add(piece.getIndex());

        if (END_GAME) {
            PieceIndexSet pending = pendingPiecesMap.get(origin);
            pending.remove(piece.getIndex());
        }

        if (!END_GAME || !endGame) {
            // end game is not enabled or not currently happening;
            // we only have to tell our peers we received a new piece
            for (BitTorrentConnection c : connections) {
                c.pieceReceived(origin, piece.getIndex());
            }
        } else {
            // end game is enabled and currently happening
            // we have to update the end game administration, and
            // either cancel a previously requested piece from a peer
            // or tell a peer we received a new piece
            for (BitTorrentConnection c : connections) {
                Object peer = c.getPeer();

                PieceIndexSet pendingPieces = pendingPiecesMap.get(peer);

                boolean wasPending = pendingPieces.remove(piece.getIndex());

                if (wasPending && !peer.equals(origin)) {
                    // cancel a previously requested piece
                    c.cancelPiece(piece.getIndex());
                } else {
                    // tell this peer we received a new piece
                    c.pieceReceived(origin, piece.getIndex());
                }

                PieceIndexSet endGamePieces = endGamePiecesMap.get(peer);
                endGamePieces.remove(piece.getIndex());
            }
        }

        // notify threads that are waiting until all pieces have been received
        if (piecesReceived.size() >= totalPieces) {
            notifyAll();
        }
    }

    public synchronized boolean areAllPieceReceived() {
        return piecesReceived.size() == totalPieces;
    }

    public synchronized void waitUntilAllPiecesReceived() {
        while (piecesReceived.size() < totalPieces) {
            logger.info("waiting until we received all pieces...");
            try {
                wait();
            } catch (InterruptedException e) {
                logger.fatal("Interrupted while waiting until we received " +
                		"all pieces");
            }
        }
    }

    public void printStats(String prefix) throws IOException {
        // do nothing
    }

}
