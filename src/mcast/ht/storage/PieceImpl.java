package mcast.ht.storage;

class PieceImpl implements Config, Piece {

    private final int index;

    PieceImpl(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String toString() {
        return "<" + index + ">";
    }

    public static String indicesToString(Piece[] pieces) {
        String result = "";
        String concat = "";
        for (int i = 0; i < pieces.length; i++) {
            result += concat + pieces[i].getIndex();
            concat = ", ";
        }
        return result;
    }

    public static String toString(Piece[] pieces) {
        if (pieces.length == 0) {
            return "none";
        } else {
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < pieces.length; i++) {
                if (i > 0) {
                    result.append(',');
                }
                result.append(pieces[i].getIndex());
            }

            return result.toString();
        }
    }

}
