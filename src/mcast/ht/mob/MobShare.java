package mcast.ht.mob;

import ibis.ipl.IbisIdentifier;

import java.util.List;

import mcast.ht.Collective;
import mcast.ht.admin.PieceIndexSet;
import mcast.ht.admin.PieceIndexSetFactory;

public class MobShare {

	private final int firstPieceIndex;
	private final int lastPieceIndex;

	public MobShare(Collective collective, IbisIdentifier ibis, int totalPieces) 
	{
	    List<IbisIdentifier> members = collective.getMembers();
		double share = totalPieces / (double)(members.size());
		int rankInCollective = members.indexOf(ibis);
		
		if (rankInCollective < 0) {
		    throw new RuntimeException("Cannot compute mob share of " + ibis + 
		            ", it is not part of collective " + collective);
		}
		
		firstPieceIndex = (int)Math.floor(rankInCollective * share);
		lastPieceIndex = (int)(Math.floor((rankInCollective + 1) * share) - 1);
	}

	public MobShare(int totalPieces) {
		firstPieceIndex = 0;
		lastPieceIndex = totalPieces - 1;
	}
		
	public int getFirstPieceIndex() {
		return firstPieceIndex;
	}
	
	public int getLastPieceIndex() {
		return lastPieceIndex;
	}
	
	public boolean contains(int pieceIndex) {
		return (pieceIndex >= firstPieceIndex) && (pieceIndex <= lastPieceIndex);
	}

	public int size() {
		return lastPieceIndex - firstPieceIndex + 1;
	}
	
	public PieceIndexSet getPieceIndices() {
		PieceIndexSet indices = PieceIndexSetFactory.createEmptyPieceIndexSet();

		indices.init(firstPieceIndex, lastPieceIndex - firstPieceIndex + 1);

		return indices;
	}

	public String toString() {
		return "[" + firstPieceIndex + "-" + lastPieceIndex + "]";
	}

}
