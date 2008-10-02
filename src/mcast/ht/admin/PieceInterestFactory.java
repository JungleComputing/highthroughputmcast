package mcast.ht.admin;

public class PieceInterestFactory {

    public static PieceInterest createPieceInterest(int totalPieces,
            PieceIndexSet silver, PieceIndexSet gold) {
        return new PieceInterestSharedObjects(totalPieces, silver, gold);
    }

}
