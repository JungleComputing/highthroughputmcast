package mcast.p2p.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

public class PieceInterestSharedObjects implements PieceInterest {

    private Logger logger = Logger.getLogger(PieceInterestSharedObjects.class);

    private final PieceIndexSet goldenPieces;
    private final PieceIndexSet silverPieces;
    private final SharedPiece[] pieces;
    private final HashMap<Object, InterestNodeList> goldMap;
    private final HashMap<Object, InterestNodeList> silverMap;
    private final Random random;

    PieceInterestSharedObjects(int capacity, PieceIndexSet silverPieces,
            PieceIndexSet goldenPieces) {
        if (logger.isTraceEnabled()) {
            logger.trace("<init>(" + capacity + ", " + silverPieces + ", "
                    + goldenPieces + ");");
        }

        this.goldenPieces = goldenPieces.deepCopy();
        this.silverPieces = silverPieces.deepCopy();

        this.silverPieces.removeAll(goldenPieces);

        pieces = new SharedPiece[capacity];
        goldMap = new HashMap<Object, InterestNodeList>();
        silverMap = new HashMap<Object, InterestNodeList>();
        random = new Random();

        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new SharedPiece(i);
        }
    }

    public synchronized int[] removeGoldOrSilver(Object peer, int amount) {
        if (logger.isTraceEnabled()) {
            logger.trace("removeGoldOrSilver(" + peer + ", " + amount + ");");
        }

        if (amount <= 0) {
            return NOTHING;
        }

        InterestNodeList gold = getList(peer, goldMap);
        InterestNodeList silver = getList(peer, silverMap);

        if (gold.size() == 0 && silver.size() == 0) {
            // currently we desire no pieces from this peer
            return NOTHING;
        } else {
            // pick the piece indices to return
            int noIndicesToPick = Math.min(amount, gold.size() + silver.size());
            int[] result = new int[noIndicesToPick];

            InterestNodeList pickList = gold;

            for (int i = 0; i < result.length; i++) {
                if (pickList.size() == 0) {
                    pickList = silver;
                }

                int index = random.nextInt(pickList.size());

                InterestNode node = pickList.getNode(index);
                SharedPiece piece = node.getPiece();

                result[i] = piece.getIndex();

                doRemove(piece.getIndex());
            }

            return result;
        }
    }

    public synchronized int[] removeGold(Object peer, int amount) {
        if (logger.isTraceEnabled()) {
            logger.trace("removeGold(" + peer + ", " + amount + ");");
        }

        if (amount <= 0) {
            return NOTHING;
        }

        InterestNodeList gold = getList(peer, goldMap);

        if (gold.size() == 0) {
            // currently we desire no pieces from this peer
            return NOTHING;
        } else {
            // pick the piece indices to return
            int noIndicesToPick = Math.min(amount, gold.size());
            int[] result = new int[noIndicesToPick];

            for (int i = 0; i < result.length; i++) {
                int index = random.nextInt(gold.size());

                InterestNode node = gold.getNode(index);
                SharedPiece piece = node.getPiece();

                result[i] = piece.getIndex();

                doRemove(piece.getIndex());
            }

            return result;
        }
    }

    public synchronized void devaluate(int pieceIndex) {
        if (goldenPieces.remove(pieceIndex)) {
            silverPieces.add(pieceIndex);
            movePieces(pieces[pieceIndex], silverMap);
        }
    }

    public synchronized void devaluate(PieceIndexSet pieceIndices) {
        for (int i : pieceIndices) {
            if (goldenPieces.remove(i)) {
                silverPieces.add(i);
                movePieces(pieces[i], silverMap);
            }
        }
    }

    public synchronized PieceIndexSet devaluateFirst(double fraction) {
        PieceIndexSet result = goldenPieces.removeFirst(fraction);

        for (int i : result) {
            silverPieces.add(i);
            movePieces(pieces[i], silverMap);
        }

        return result;
    }

    public synchronized void revaluate(PieceIndexSet pieceIndices) {
        for (int i : pieceIndices) {
            if (silverPieces.remove(i)) {
                goldenPieces.add(i);
                movePieces(pieces[i], goldMap);
            }
        }
    }

    public synchronized boolean tellHave(Object peer, int pieceIndex,
            boolean wantOnlyGold)
    {
        if (logger.isTraceEnabled()) {
            logger.trace("tellHave(" + peer + ", " + pieceIndex + ", "
                    + wantOnlyGold + ");");
        }

        if (pieces.length == 0) {
            return false;
        }

        if (goldenPieces.contains(pieceIndex)) {
            InterestNodeList gold = getList(peer, goldMap);
            gold.addPiece(pieces[pieceIndex]);
            return true;
        } else if (silverPieces.contains(pieceIndex)) {
            InterestNodeList silver = getList(peer, silverMap);
            silver.addPiece(pieces[pieceIndex]);
            return !wantOnlyGold;
        }

        return false;
    }

    public synchronized boolean tellHave(Object peer,
            PieceIndexSet pieceIndices, boolean wantOnlyGold)
    {
        if (logger.isTraceEnabled()) {
            logger.trace("tellHave(" + peer + ", " + pieceIndices + ", "
                    + wantOnlyGold + ");");
        }

        if (pieces.length == 0) {
            return false;
        }

        boolean result = false;

        InterestNodeList gold = getList(peer, goldMap);
        InterestNodeList silver = getList(peer, silverMap);

        for (Integer pieceIndex : pieceIndices) {
            SharedPiece piece = pieces[pieceIndex.intValue()];

            if (goldenPieces.contains(piece.getIndex())) {
                gold.addPiece(piece);
                result |= true;
            } else if (silverPieces.contains(piece.getIndex())) {
                silver.addPiece(piece);
                result |= !wantOnlyGold;
            }
        }

        return result;
    }

    private void movePieces(SharedPiece piece, Map<Object, InterestNodeList> destMap) {
        for (InterestNode node : piece.getNodes()) {
            InterestNodeList destList = getList(node.getPeer(), destMap);
            node.setOwner(destList);
        }	
    }

    public synchronized boolean isEmpty() {
        return goldenPieces.isEmpty() && silverPieces.isEmpty();
    }

    public synchronized boolean containsGold() {
        return !goldenPieces.isEmpty();
    }

    public synchronized boolean containsSilver() {
        return !silverPieces.isEmpty();
    }

    public synchronized void remove(int pieceIndex) {
        if (logger.isTraceEnabled()) {
            logger.trace("remove(" + pieceIndex + ");");
        }

        doRemove(pieceIndex);
    }

    private void doRemove(int pieceIndex) {
        goldenPieces.remove(pieceIndex);
        silverPieces.remove(pieceIndex);
        pieces[pieceIndex].forget();
    }

    public synchronized PieceIndexSet getGold(Object peer) {
        return getPieces(peer, goldMap);
    }

    public synchronized PieceIndexSet getGold() {
        return goldenPieces.deepCopy();
    }

    public synchronized PieceIndexSet getSilver() {
        return silverPieces.deepCopy();
    }

    public synchronized PieceIndexSet getSilver(Object peer) {
        return getPieces(peer, silverMap);
    }

    private PieceIndexSet getPieces(Object peer, Map<Object, InterestNodeList> map) {
        InterestNodeList pieces = getList(peer, map);

        if (pieces == null) {
            return PieceIndexSetFactory.createEmptyPieceIndexSet();
        } else {
            return pieces.getPieceIndices();
        }
    }

    private InterestNodeList getList(Object peer, Map<Object, InterestNodeList> list) {
        InterestNodeList result = list.get(peer);

        // add node list for the peer if it does not have one yet
        if (result == null) {
            result = new InterestNodeList(peer);
            list.put(peer, result);
        }

        return result;
    }

    public synchronized String toString() {
        StringBuilder result = new StringBuilder();

        result.append("gold=");
        result.append(goldenPieces);
        result.append(", silver=");
        result.append(silverPieces);

        for (Map.Entry<Object, InterestNodeList> entry : goldMap.entrySet()) {
            result.append('\n');
            result.append(entry.getKey());
            result.append("-gold=");
            result.append(entry.getValue().size());
            result.append(':');
            result.append(entry.getValue().getPieceIndices());
        }

        for (Map.Entry<Object, InterestNodeList> entry : silverMap.entrySet()) {
            result.append('\n');
            result.append(entry.getKey());
            result.append("-silver=");
            result.append(entry.getValue().size());
            result.append(':');
            result.append(entry.getValue().getPieceIndices());
        }

        return result.toString();
    }

}
