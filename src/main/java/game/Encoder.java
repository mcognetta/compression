package org.lichess.compression.game;

import java.util.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.nio.ByteBuffer;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Encoder {
    private static final ThreadLocal<MoveList> moveList = new ThreadLocal<MoveList>() {
        @Override
        protected MoveList initialValue() {
            return new MoveList();
        }
    };

    private static Pattern SAN_PATTERN = Pattern.compile(
        "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?");

    private static Role charToRole(char c) {
        switch (c) {
            case 'N': return Role.KNIGHT;
            case 'B': return Role.BISHOP;
            case 'R': return Role.ROOK;
            case 'Q': return Role.QUEEN;
            case 'K': return Role.KING;
            default: throw new IllegalArgumentException();
        }
    }

    public static byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Board board = new Board();
        MoveList legals = moveList.get();

        for (String pgnMove: pgnMoves) {
            // Parse SAN.
            Role role = null, promotion = null;
            long from = Bitboard.ALL;
            int to;

            if (pgnMove.startsWith("O-O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
            } else if (pgnMove.startsWith("O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ?  0 : 7]);
            } else {
                Matcher matcher = SAN_PATTERN.matcher(pgnMove);

                if (!matcher.matches()) {System.out.println("FAILED MATCH"); return null; }

                String roleStr = matcher.group(1);
                role = roleStr == null ? Role.PAWN : charToRole(roleStr.charAt(0));

                if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
                if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

                to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

                if (matcher.group(5) != null) {
                    promotion = charToRole(matcher.group(5).charAt(0));
                }
            }

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(from, legal.from)) {
                    if (!foundMatch) {
                        // Encode and play.
                        Huffman.write(i, writer);
                        board.play(legal);
                        foundMatch = true;
                    }
                    else {
                         System.out.println("NO MATCH2"); return null;
                    }
                }
            }

            if (!foundMatch) {System.out.println("NO MATCH2"); return null;}
        }

        return writer.toArray();
    }


    public static ArrayList<String> stringencode(String pgnMoves[]) {
        System.out.println(pgnMoves != null);

        ArrayList<String> moves = new ArrayList<String>();

        Board board = new Board();
        MoveList legals = moveList.get();

        for (String pgnMove: pgnMoves) {
            // Parse SAN.
            Role role = null, promotion = null;
            long from = Bitboard.ALL;
            int to;

            if (pgnMove.startsWith("O-O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
                // System.out.println("0-0-0: " + Long.toString(from) + " " + Integer.toString(to));
            } else if (pgnMove.startsWith("O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ?  0 : 7]);
                // System.out.println("0-0: " + Long.toString(from) + " " + Integer.toString(to));
            } else {
                Matcher matcher = SAN_PATTERN.matcher(pgnMove);
                if (!matcher.matches()) {System.out.println("FAILED MATCH"); return null;}

                String roleStr = matcher.group(1);
                role = roleStr == null ? Role.PAWN : charToRole(roleStr.charAt(0));

                if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
                if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

                to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

                if (matcher.group(5) != null) {
                    promotion = charToRole(matcher.group(5).charAt(0));
                }
            }

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(from, legal.from)) {
                    if (!foundMatch) {
                        // Encode and play.
                        moves.add(BITCODES[i]);
                        board.play(legal);
                        foundMatch = true;
                    }
                    else {System.out.println("NO MATCH"); return null;}
                }
            }

            if (!foundMatch) {System.out.println("NO MATCH2"); return null;}
        }

        return moves;
    }
    public static ArrayList<String> stringencodescores(String pgnMoves[]) {

        ArrayList<String> moves = new ArrayList<String>();

        Board board = new Board();
        MoveList legals = moveList.get();

        int tot = 0;
        for (String pgnMove: pgnMoves) {
            // Parse SAN.
            Role role = null, promotion = null;
            long from = Bitboard.ALL;
            int to;

            if (pgnMove.startsWith("O-O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
            } else if (pgnMove.startsWith("O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ?  0 : 7]);
            } else {
                Matcher matcher = SAN_PATTERN.matcher(pgnMove);
                if (!matcher.matches()) { System.out.println("MATCH FAILED: " + pgnMove); return null;}

                String roleStr = matcher.group(1);
                role = roleStr == null ? Role.PAWN : charToRole(roleStr.charAt(0));

                if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
                if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

                to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

                if (matcher.group(5) != null) {
                    promotion = charToRole(matcher.group(5).charAt(0));
                }
            }

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort();

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(from, legal.from)) {
                    if (!foundMatch) {
                        // Encode and play.
                        // moves.add(legal.uci() + ", " + pgnMove + ", " + Integer.toString(i) + ", " + BITCODES[i]);
                        String score_bits =  Integer.toBinaryString(legal.score);
                        if (score_bits.length() < 32) {
                            score_bits = "0".repeat(32 - score_bits.length()) + score_bits;
                        }
                        // if (pgnMove.startsWith("Qff7")) {
                        //     List<Move> legal_moves = legals.getMoveList();
                        //     List<String> names = legal_moves.stream().map(v -> v.uci()).map(String.class::cast).collect(Collectors.toList());
                        //     List<String> scores = legal_moves.stream().map(v -> Integer.toString(v.score)).collect(Collectors.toList());

                        //     System.out.println(String.join(", ", names ));
                        //     System.out.println(String.join(", ", scores));

                        // }
                        // System.out.println(pgnMove + ", " + Integer.toString(i) + ", " + BITCODES[i] + ", " + score_bits);
                        moves.add(BITCODES[i].substring(2));

                        board.play(legal);
                        tot += BITCODES[i].length() - 2;
                        foundMatch = true;
                    }
                    else {System.out.println("NO MATCH"); return null;}
                }
            }

            if (!foundMatch) {System.out.println("NO MATCH2"); return null;}
        }
        // System.out.println(tot);
        return moves;
    }

    public static ArrayList<String> getBestMoves(int max_moves) {

        ArrayList<String> moves = new ArrayList<String>();

        Board board = new Board();
        MoveList legals = moveList.get();

        for (int i = 0; i < max_moves; i++) {
            board.legalMoves(legals);
            legals.sort();
            // Move legal = legals.get(legals.size()-1);
            Move legal = legals.get(0);
            moves.add(legal.uci() + ", " + Integer.toString(legal.score) + ", 0, 0b00");
            board.play(legal);
        }
        return moves;
    }

    public static class DecodeResult {
        public final String pgnMoves[];
        public final Map<Integer, Piece> pieces;
        public final Set<Integer> unmovedRooks;
        public final int halfMoveClock;
        public final byte positionHashes[];
        public final String lastUci;

        public DecodeResult(String pgnMoves[], Map<Integer, Piece> pieces, Set<Integer> unmovedRooks, int halfMoveClock, byte positionHashes[], String lastUci) {
            this.pgnMoves = pgnMoves;
            this.pieces = pieces;
            this.unmovedRooks = unmovedRooks;
            this.halfMoveClock = halfMoveClock;
            this.positionHashes = positionHashes;
            this.lastUci = lastUci;
        }
    }

    public static DecodeResult decode(byte input[], int plies) {
        BitReader reader = new BitReader(input);

        String output[] = new String[plies];

        Board board = new Board();
        MoveList legals = moveList.get();

        String lastUci = null;

        // Collect the position hashes (3 bytes each) since the last capture
        // or pawn move.
        int lastZeroingPly = -1;
        int lastIrreversiblePly = -1;
        byte positionHashes[] = new byte[3 * (plies + 1)];
        setHash(positionHashes, -1, board.zobristHash());

        for (int i = 0; i <= plies; i++) {
            if (0 < i || i < plies) board.legalMoves(legals);

            // Append check or checkmate suffix to previous move.
            if (0 < i) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            // Decode and play next move.
            if (i < plies) {
                legals.sort();
                Move move = legals.get(Huffman.read(reader));
                output[i] = san(move, legals);
                board.play(move);

                if (move.isZeroing()) lastZeroingPly = i;
                if (move.isIrreversible()) lastIrreversiblePly = i;
                setHash(positionHashes, i, board.zobristHash());

                if (i + 1 == plies) lastUci = move.uci();
            }
        }

        return new DecodeResult(
            output,
            board.pieceMap(),
            Bitboard.squareSet(board.castlingRights),
            plies - 1 - lastZeroingPly,
            Arrays.copyOf(positionHashes, 3 * (plies - lastIrreversiblePly)),
            lastUci);
    }

    private static String san(Move move, MoveList legals) {
        switch (move.type) {
            case Move.NORMAL:
            case Move.EN_PASSANT:
                StringBuilder builder = new StringBuilder(6);
                builder.append(move.role.symbol);

                // From.
                if (move.role != Role.PAWN) {
                    boolean file = false, rank = false;
                    long others = 0;

                    for (int i = 0; i < legals.size(); i++) {
                        Move other = legals.get(i);
                        if (other.role == move.role && other.to == move.to && other.from != move.from) {
                            others |= 1L << other.from;
                        }
                    }

                    if (others != 0) {
                        if ((others & Bitboard.RANKS[Square.rank(move.from)]) != 0) file = true;
                        if ((others & Bitboard.FILES[Square.file(move.from)]) != 0) rank = true;
                        else file = true;
                    }

                    if (file) builder.append((char) (Square.file(move.from) + 'a'));
                    if (rank) builder.append((char) (Square.rank(move.from) + '1'));
                } else if (move.capture) {
                    builder.append((char) (Square.file(move.from) + 'a'));
                }

                // Capture.
                if (move.capture) builder.append('x');

                // To.
                builder.append((char) (Square.file(move.to) + 'a'));
                builder.append((char) (Square.rank(move.to) + '1'));

                // Promotion.
                if (move.promotion != null) {
                    builder.append('=');
                    builder.append(move.promotion.symbol);
                }

                return builder.toString();

            case Move.CASTLING:
                return move.from < move.to ? "O-O" : "O-O-O";
        }

        return "--";
    }

    private static void setHash(byte buffer[], int ply, int hash) {
        // The hash for the starting position (ply = -1) goes last. The most
        // recent position goes first.
        int base = buffer.length - 3 * (ply + 1 + 1);
        buffer[base] = (byte) (hash >>> 16);
        buffer[base + 1] = (byte) (hash >>> 8);
        buffer[base + 2] = (byte) hash;
    }

    private static final String BITCODES[] = {
        "0b00","0b100","0b1101","0b1010","0b0101","0b11101","0b10111","0b01110","0b01100","0b01000","0b111101","0b111001","0b111100","0b110011","0b110010","0b110000","0b101101","0b101100","0b011111","0b011011","0b010011","0b011010","0b1111111","0b1111101","0b1111110","0b1111100","0b1110000","0b1100011","0b0111101","0b0100101","0b0100100","0b11100010","0b11000101","0b01111001","0b111000111","0b110001001","0b011110001","0b011110000","0b1110001100","0b1100010000","0b11100011010","0b11000100010","0b111000110110","0b110001000110","0b1110001101110","0b1100010001110","0b11100011011110","0b11000100011110","0b111000110111110","0b110001000111110","0b1110001101111110","0b1100010001111110","0b11000100011111111","0b111000110111111111","0b111000110111111101","0b110001000111111100","0b1110001101111111100","0b1100010001111111011","0b11100011011111111011","0b11100011011111110010","0b11100011011111110000","0b111000110111111110101","0b111000110111111100110","0b111000110111111100010","0b110001000111111101001","0b110001000111111101000","0b1110001101111111101000","0b1110001101111111000110","0b1100010001111111010111","0b1100010001111111010101","0b11100011011111111010011","0b11100011011111110011110","0b11100011011111110001110","0b11100011011111110001111","0b11000100011111110101100","0b111000110111111100111011","0b111000110111111110100100","0b111000110111111100111111","0b111000110111111100111010","0b110001000111111101011011","0b110001000111111101010011","0b110001000111111101010001","0b1110001101111111001110011","0b1110001101111111001110001","0b1110001101111111001110010","0b1100010001111111010100101","0b1100010001111111010110100","0b1100010001111111010100001","0b11100011011111110011111011","0b11100011011111110011111001","0b11100011011111110011111010","0b11100011011111110011111000","0b11000100011111110101101011","0b111000110111111110100101111","0b110001000111111101011010100","0b110001000111111101011010101","0b111000110111111100111000010","0b111000110111111100111000011","0b110001000111111101010010011","0b1110001101111111101001010011","0b1100010001111111010100100101","0b1110001101111111001110000011","0b1110001101111111001110000010","0b1110001101111111001110000000","0b11100011011111110011100000010","0b11000100011111110101000001001","0b11100011011111110011100000011","0b11000100011111110101000001000","0b11000100011111110101000000011","0b110001000111111101010000011110","0b111000110111111110100101100110","0b111000110111111110100101010111","0b110001000111111101010000001101","0b111000110111111110100101100010","0b110001000111111101010000001000","0b110001000111111101010000000101","0b110001000111111101010000000000","0b110001000111111101010000001010","0b110001000111111101010010001101","0b110001000111111101010010010011","0b110001000111111101010010010010","0b110001000111111101010010010001","0b110001000111111101010010010000","0b110001000111111101010010001011","0b110001000111111101010010001010","0b110001000111111101010010001001","0b110001000111111101010010001000","0b110001000111111101010010000111","0b110001000111111101010010000110","0b110001000111111101010010000011","0b110001000111111101010010000010","0b110001000111111101010000011011","0b110001000111111101010000011010","0b110001000111111101010000011001","0b110001000111111101010000011000","0b110001000111111101010000010101","0b110001000111111101010000010100","0b110001000111111101010010000101","0b110001000111111101010010000100","0b110001000111111101010000011111","0b110001000111111101010000011101","0b110001000111111101010000011100","0b110001000111111101010010000001","0b110001000111111101010010000000","0b110001000111111101010000001111","0b110001000111111101010000001110","0b110001000111111101010000001100","0b110001000111111101010000010111","0b110001000111111101010000010110","0b110001000111111101010000001001","0b110001000111111101010000000100","0b110001000111111101010000000011","0b110001000111111101010000000010","0b110001000111111101010000000001","0b110001000111111101010000001011","0b110001000111111101010010001111","0b110001000111111101010010001110","0b110001000111111101010010001100","0b1110001101111111101001010111101","0b1110001101111111101001010111111","0b1110001101111111101001010100010","0b1110001101111111101001011011111","0b1110001101111111101001010100100","0b1110001101111111101001010111001","0b1110001101111111101001011011010","0b1110001101111111101001011010010","0b1110001101111111101001011010000","0b1110001101111111101001010111010","0b1110001101111111101001010001011","0b1110001101111111101001010001010","0b1110001101111111101001010001001","0b1110001101111111101001010001000","0b1110001101111111101001010000111","0b1110001101111111101001010000110","0b1110001101111111101001010000101","0b1110001101111111101001010000100","0b1110001101111111101001011010111","0b1110001101111111101001011010110","0b1110001101111111101001011010101","0b1110001101111111101001011010100","0b1110001101111111101001010110111","0b1110001101111111101001010110110","0b1110001101111111101001010010101","0b1110001101111111101001010010100","0b1110001101111111101001010110101","0b1110001101111111101001010110100","0b1110001101111111101001010010111","0b1110001101111111101001010010110","0b1110001101111111101001010110001","0b1110001101111111101001010110000","0b1110001101111111101001010010011","0b1110001101111111101001010010010","0b1110001101111111101001011101101","0b1110001101111111101001011101100","0b1110001101111111101001011101011","0b1110001101111111101001011101010","0b1110001101111111101001011100111","0b1110001101111111101001011100110","0b1110001101111111101001010010001","0b1110001101111111101001010010000","0b1110001101111111101001011100011","0b1110001101111111101001011100010","0b1110001101111111101001011100001","0b1110001101111111101001011100000","0b1110001101111111101001011101001","0b1110001101111111101001011101000","0b1110001101111111101001010001111","0b1110001101111111101001010001110","0b1110001101111111101001010000011","0b1110001101111111101001010000010","0b1110001101111111101001010001101","0b1110001101111111101001010001100","0b1110001101111111101001011001111","0b1110001101111111101001011001110","0b1110001101111111101001010000001","0b1110001101111111101001010000000","0b1110001101111111101001011011001","0b1110001101111111101001011011000","0b1110001101111111101001011100101","0b1110001101111111101001011100100","0b1110001101111111101001010101101","0b1110001101111111101001010101100","0b1110001101111111101001010110011","0b1110001101111111101001010110010","0b1110001101111111101001010101001","0b1110001101111111101001010101000","0b1110001101111111101001011101111","0b1110001101111111101001011101110","0b1110001101111111101001011001011","0b1110001101111111101001011001010","0b1110001101111111101001011000011","0b1110001101111111101001011000010","0b1110001101111111101001010101011","0b1110001101111111101001010101010","0b1110001101111111101001011001001","0b1110001101111111101001011001000","0b1110001101111111101001011000111","0b1110001101111111101001011000110","0b1110001101111111101001011000001","0b1110001101111111101001011000000","0b1110001101111111101001010111100","0b1110001101111111101001010100111","0b1110001101111111101001010100110","0b1110001101111111101001010111110","0b1110001101111111101001010100011","0b1110001101111111101001010100001","0b1110001101111111101001010100000","0b1110001101111111101001011011110","0b1110001101111111101001010100101","0b1110001101111111101001011011101","0b1110001101111111101001011011100","0b1110001101111111101001010111000","0b1110001101111111101001011011011","0b1110001101111111101001011010001","0b1110001101111111101001011010011","0b1110001101111111101001010111011"};
}
