package mcast.p2p.robber;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;

import java.io.IOException;

import mcast.p2p.admin.PieceIndexSet;
import mcast.p2p.admin.PieceIndexSetFactory;
import mcast.p2p.bittorrent.BitTorrentCommunicator;

import org.apache.log4j.Logger;

public class RobberCommunicator extends BitTorrentCommunicator 
implements Config
{

    private static Logger logger = Logger.getLogger(RobberCommunicator.class);

    private static final byte OPCODE_DESIRE = 11;
    private static final byte OPCODE_STEAL = 12;
    private static final byte OPCODE_WORK = 13;
    private static final byte OPCODE_FOUND_WORK = 14;

    private RobberUpcall upcall;

    public RobberCommunicator(IbisIdentifier me, IbisIdentifier peer, 
            RobberUpcall upcall) {
        super(me, peer, upcall, false, false);

        this.upcall = upcall;
    }

    void sendDesire(PieceIndexSet pieceIndices) throws IOException {
        sendPieceIndexSetMessage(OPCODE_DESIRE, pieceIndices);
    }

    void sendFoundWork() throws IOException {
        sendMessage(OPCODE_FOUND_WORK);
    }

    void sendSteal() throws IOException {
        sendMessage(OPCODE_STEAL);
    }

    void sendSteal(int piecesReceived) throws IOException {
        sendMessage(OPCODE_STEAL, piecesReceived);
    }

    void sendWork(PieceIndexSet pieceIndices) throws IOException {
        sendPieceIndexSetMessage(OPCODE_WORK, pieceIndices);
    }

    @Override
    protected String messageName(int opcode) {
        switch (opcode) {
        case OPCODE_DESIRE:
            return "desire";
        case OPCODE_STEAL:
            return "steal";
        case OPCODE_WORK:
            return "work";
        default:
            return super.messageName(opcode);
        }
    }

    @Override
    public void handleMessage(byte opcode, ReadMessage m) throws IOException {
        switch (opcode) {
        case OPCODE_DESIRE: {
            PieceIndexSet pieceIndices = PieceIndexSetFactory.readPieceIndexSet(m);
            if (logger.isTraceEnabled()) {
                logger.trace("R desire " + pieceIndices);
            }
            upcall.receiveDesire(pieceIndices);
            break;
        }
        case OPCODE_STEAL: {
            if (logger.isTraceEnabled()) {
                logger.trace("R steal");
            }

            if (BALANCE_BOOTY) {
                int piecesReceived = m.readInt();
                upcall.receiveSteal(piecesReceived);
            } else {
                upcall.receiveSteal();
            }

            break;
        }
        case OPCODE_WORK: {
            PieceIndexSet pieceIndices = PieceIndexSetFactory
            .readPieceIndexSet(m);
            if (logger.isTraceEnabled()) {
                logger.trace("R work " + pieceIndices);
            }
            upcall.receiveWork(pieceIndices);
            break;
        }
        case OPCODE_FOUND_WORK: {
            if (logger.isTraceEnabled()) {
                logger.trace("R found_work");
            }
            upcall.receiveFoundWork();
            break;
        }
        default:
            // we received a BitTorrent message
            super.handleMessage(opcode, m);
        }
    }

}
