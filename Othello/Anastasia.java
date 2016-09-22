import java.util.ArrayList;

// Created by Michal, Winston and Long

public class Anastasia extends OthelloPlayer {
    private int MIN_MAX_DEPTH = 15;

    private Integer BLACK = OthelloBoard.BLACK;
    private Integer WHITE = OthelloBoard.WHITE;

    public Anastasia(Integer _color) {
        super(_color);
    }

    private int flipPlayer(int player) {
        return player == WHITE ? BLACK : WHITE;
    }

    private OthelloBoard cloneBoard(OthelloBoard toClone) {
        OthelloBoard result = new OthelloBoard(toClone.size, false);
        for (int i = 0; i < toClone.size; ++i) {
            for (int j = 0; j < toClone.size; ++j) {
                result.board[i][j] = toClone.board[i][j];
            }
        }
        return result;
    }

    private double checkerCountHeuristic(OthelloBoard board, int player) {
        int blackAdvantage = 0;
        int totalCount = 0;
        for (int r = 0; r < board.size; r++) {
            for (int c = 0; c < board.size; c++) {
                if (board.board[r][c] == BLACK) {
                    blackAdvantage++;
                    totalCount++;
                }
                else if (board.board[r][c] == WHITE) {
                    blackAdvantage--;
                    totalCount++;
                }
            }
        }
        return ((double)blackAdvantage)/totalCount;
    }

    double[][] checkersWeights = null;

    private void prepareWeights(int size) {
        checkersWeights = new double[size][size];

        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                checkersWeights[x][y] = 1;
//                if (x == 0 || y == 0 || x == size-1 || y == size-1)
//                    checkersWeights[x][y] *= 1.6;
//                double leftProximityBonus = (size/2)- x;
//                double topProximityBonus = (size/2) - y;
//                double rightProximityBonus = (size/2) - (size-1-x);
//                double bottomProximityBonus = (size/2) - (size-1-y);
//                if (leftProximityBonus < 0)
//                    leftProximityBonus = 0;
//                if (rightProximityBonus < 0)
//                    rightProximityBonus = 0;
//                if (topProximityBonus < 0)
//                    topProximityBonus = 0;
//                if (bottomProximityBonus < 0)
//                    bottomProximityBonus = 0;
//                leftProximityBonus *= leftProximityBonus;
//                rightProximityBonus *= rightProximityBonus;
//                topProximityBonus *= topProximityBonus;
//                bottomProximityBonus *= bottomProximityBonus;
//
//                checkersWeights[x][y] = size*0.5 + leftProximityBonus + rightProximityBonus + topProximityBonus + bottomProximityBonus;
            }
        }
        checkersWeights[0][0] *= 15;
        checkersWeights[size-1][size-1] *= 15;
        checkersWeights[size-1][0] *= 15;
        checkersWeights[0][size-1] *= 15;
    }

    private double weightedCheckerCountHeuristic(OthelloBoard board, int player) {
        double blackAdvantage = 0;
        double totalCount = 0;
        for (int r = 0; r < board.size; r++) {
            for (int c = 0; c < board.size; c++) {
                if (board.board[r][c] == BLACK) {
                    double weight = checkersWeights[r][c];
                    blackAdvantage += weight;
                    totalCount += weight;
                }
                else if (board.board[r][c] == WHITE) {
                    double weight = checkersWeights[r][c];
                    blackAdvantage -= weight;
                    totalCount += weight;
                }
            }
        }
        return blackAdvantage/totalCount;
    }

    private double movesAvailableHeuristic(OthelloBoard board, int player) {
        int blackAvailable = board.legalMoves(BLACK).size();
        int whiteAvailable = board.legalMoves(WHITE).size();
        return ((double)(blackAvailable-whiteAvailable))/(blackAvailable+whiteAvailable);
    }

    private boolean emptyField(int player) {
        return player != WHITE && player != BLACK;
    }

    private class StabilityResult {
        public int stableBlack;
        public int unstableBlack;
        public int volatileBlack;
        public int stableWhite;
        public int unstableWhite;
        public int volatileWhite;
        public int blackFrontier;
        public int whiteFrontier;

        public StabilityResult(int stableBlack, int unstableBlack, int volatileBlack, int stableWhite,
                               int unstableWhite, int volatileWhite, int blackFrontier, int whiteFrontier) {
            this.stableBlack = stableBlack;
            this.stableWhite = stableWhite;
            this.volatileBlack = volatileBlack;
            this.volatileWhite = volatileWhite;
            this.unstableBlack = unstableBlack;
            this.unstableWhite = unstableWhite;
            this.blackFrontier = blackFrontier;
            this.whiteFrontier = whiteFrontier;
        }
        public StabilityResult () {
            this.stableBlack = 0;
            this.stableWhite = 0;
            this.volatileBlack = 0;
            this.volatileWhite = 0;
            this.unstableBlack = 0;
            this.unstableWhite = 0;
            this.blackFrontier = 0;
            this.whiteFrontier = 0;
        }
    }

    private StabilityResult stabilityCount(OthelloBoard board, int player) {
        int EMPTY = 0;
        int STABLE= 1;
        int UNSTABLE = -1;
        int VOLATILE = -2;

        int[][] stabilityBoard = new int[board.size][board.size];

        StabilityResult result = new StabilityResult();

        for (int x = 0; x < board.size; ++x) {
            for (int y = 0; y < board.size; ++y) {
                boolean isFrontier = false;
                if (!emptyField(board.board[y][x])) {
                    stabilityBoard[y][x] = STABLE;
                    // check for empty boxes nearby
                    if (x > 0) {
                        // to left
                        if (emptyField(board.board[y][x-1])) {
                            isFrontier = true;
                            for (int x2 = x+1; x2 < board.size; ++x2) {
                                if (board.board[y][x2] != board.board[y][x]) {
                                    int setUpWith = board.board[y][x2] == EMPTY ? UNSTABLE : VOLATILE;
                                    for (int x3 = x2-1; x3 >= x; --x3) {
                                        stabilityBoard[y][x3] = setUpWith;
                                    }
                                    break;
                                }
                            }
                        }

                        // to left-up
                        if (y > 0) {
                            if (emptyField(board.board[y-1][x - 1])) {
                                isFrontier = true;
                                for (int diag = 1; x+diag < board.size && y+diag < board.size; ++diag) {
                                    if (board.board[y+diag][x+diag] != board.board[y][x]) {
                                        int setUpWith = board.board[y+diag][x+diag] == EMPTY ? UNSTABLE : VOLATILE;
                                        for (diag = diag-1; diag >= 0; --diag) {
                                            stabilityBoard[y+diag][x+diag] = setUpWith;
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        // to left-down
                        if (y < board.size-1) {
                            if (emptyField(board.board[y+1][x - 1])) {
                                isFrontier = true;
                                for (int diag = 1; x+diag < board.size && y-diag >= 0; ++diag) {
                                    if (board.board[y-diag][x+diag] != board.board[y][x]) {
                                        int setUpWith = board.board[y-diag][x+diag] == EMPTY ? UNSTABLE : VOLATILE;
                                        for (diag = diag-1; diag >= 0; --diag) {
                                            stabilityBoard[y-diag][x+diag] = setUpWith;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (x < board.size-1) {
                        // to right
                        if (emptyField(board.board[y][x+1])) {
                            isFrontier = true;
                            for (int x2 = x-1; x2 >= 0; --x2) {
                                if (board.board[y][x2] != board.board[y][x]) {
                                    int setUpWith = board.board[y][x2] == EMPTY ? UNSTABLE : VOLATILE;
                                    for (int x3 = x2+1; x3 <= x; ++x3) {
                                        stabilityBoard[y][x3] = setUpWith;
                                    }
                                    break;
                                }
                            }
                        }

                        // to right-up
                        if (y > 0) {
                            if (emptyField(board.board[y-1][x + 1])) {
                                isFrontier = true;
                                for (int diag = 1; x-diag >= 0 && y+diag < board.size; ++diag) {
                                    if (board.board[y+diag][x-diag] != board.board[y][x]) {
                                        int setUpWith = board.board[y+diag][x-diag] == EMPTY ? UNSTABLE : VOLATILE;
                                        for (diag = diag-1; diag >= 0; --diag) {
                                            stabilityBoard[y+diag][x-diag] = setUpWith;
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        // to right-down
                        if (y < board.size-1) {
                            if (emptyField(board.board[x + 1][y+1])) {
                                isFrontier = true;
                                for (int diag = 1; x-diag >= 0 && y-diag >= 0; ++diag) {
                                    if (board.board[y-diag][x-diag] != board.board[y][x]) {
                                        int setUpWith = board.board[y-diag][x-diag] == EMPTY ? UNSTABLE : VOLATILE;
                                        for (diag = diag-1; diag >= 0; --diag) {
                                            stabilityBoard[y-diag][x-diag] = setUpWith;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if (y < board.size-1) {
                        // down
                        if (emptyField(board.board[y+1][x])) {
                            for (int y2 = y-1; y2 >= 0; --y2) {
                                isFrontier = true;
                                if (board.board[y2][x] != board.board[y][x]) {
                                    int setUpWith = board.board[y2][x] == EMPTY ? UNSTABLE : VOLATILE;
                                    for (int y3 = y2+1; y3 <= y; ++y3) {
                                        stabilityBoard[y3][x] = setUpWith;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (y > 0) {
                        // up
                        if (emptyField(board.board[y-1][x])) {
                            for (int y2 = y+1; y2 < board.size; ++y2) {
                                isFrontier = true;
                                if (board.board[y2][x] != board.board[y2][x]) {
                                    int setUpWith = board.board[y2][x] == EMPTY ? UNSTABLE : VOLATILE;
                                    for (int y3 = y2-1; y3 >= y; --y3) {
                                        stabilityBoard[y3][x] = setUpWith;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    stabilityBoard[y][x] = EMPTY;
                }
                if (isFrontier) {
                    if (board.board[y][x] == WHITE)
                        result.whiteFrontier++;
                    else
                        result.blackFrontier++;
                }
            }
        }

        for (int x = 0; x < board.size; ++x) {
            for (int y = 0; y < board.size; ++y) {
                if (stabilityBoard[x][y] == VOLATILE) {
                    if (board.board[x][y] == WHITE)
                        result.volatileWhite++;
                    else
                        result.volatileBlack++;
                } else if (stabilityBoard[x][y] == UNSTABLE) {
                    if (board.board[x][y] == WHITE)
                        result.unstableWhite++;
                    else
                        result.unstableBlack++;
                } else if (stabilityBoard[x][y] == STABLE) {
                    if (board.board[x][y] == WHITE)
                        result.stableWhite++;
                    else
                        result.stableBlack++;
                }
            }
        }

        return result;
    }

    public double frontierHeuristic(OthelloBoard board, int player) {
        int whiteFrontier = 0;
        int blackFrontier = 0;
        for (int x = 0; x < board.size; x++) {
            for (int y = 0; y < board.size; y++) {
                if (x > 0) {
                    if (board.board[x-1][y] == 0) {
                        if (board.board[x][y] == WHITE) {
                            whiteFrontier++;
                            continue;
                        } else if (board.board[x][y] == BLACK) {
                            blackFrontier++;
                            continue;
                        }
                    }
                    if (y > 0) {
                        if (board.board[x-1][y-1] == 0) {
                            if (board.board[x][y] == WHITE) {
                                whiteFrontier++;
                                continue;
                            } else if (board.board[x][y] == BLACK) {
                                blackFrontier++;
                                continue;
                            }
                        }
                    }
                    if (y < board.size-1) {
                        if (board.board[x-1][y+1] == 0) {
                            if (board.board[x][y] == WHITE) {
                                whiteFrontier++;
                                continue;
                            } else if (board.board[x][y] == BLACK) {
                                blackFrontier++;
                                continue;
                            }
                        }
                    }
                }
                if (x < board.size - 1) {
                    if (board.board[x+1][y] == 0) {
                        if (board.board[x][y] == WHITE) {
                            whiteFrontier++;
                            continue;
                        } else if (board.board[x][y] == BLACK) {
                            blackFrontier++;
                            continue;
                        }
                    }
                    if (y > 0) {
                        if (board.board[x+1][y-1] == 0) {
                            if (board.board[x][y] == WHITE) {
                                whiteFrontier++;
                                continue;
                            } else if (board.board[x][y] == BLACK) {
                                blackFrontier++;
                                continue;
                            }
                        }
                    }
                    if (y < board.size-1) {
                        if (board.board[x+1][y+1] == 0) {
                            if (board.board[x][y] == WHITE) {
                                whiteFrontier++;
                                continue;
                            } else if (board.board[x][y] == BLACK) {
                                blackFrontier++;
                                continue;
                            }
                        }
                    }
                }
                if (y > 0 && board.board[x][y-1] == 0) {
                    if (board.board[x][y] == WHITE) {
                        whiteFrontier++;
                        continue;
                    } else if (board.board[x][y] == BLACK) {
                        blackFrontier++;
                        continue;
                    }
                }
                if (y < board.size - 1 && board.board[x][y+1] == 0) {
                    if (board.board[x][y] == WHITE) {
                        whiteFrontier++;
                        continue;
                    } else if (board.board[x][y] == BLACK) {
                        blackFrontier++;
                        continue;
                    }
                }
            }
        }
        return ((double)(whiteFrontier - blackFrontier))/(whiteFrontier+blackFrontier);
    }

    public double stabilityHeuristics(OthelloBoard board, int player) {
        StabilityResult stability = stabilityCount(board, player);
        double stableScore = ((double)(stability.stableBlack - stability.stableWhite))/(stability.stableWhite+stability.stableBlack);
        double volatileScore = ((double)(stability.volatileWhite-stability.volatileBlack))/(stability.volatileWhite+stability.volatileBlack);
        double frontierScore = ((double)(stability.whiteFrontier-stability.blackFrontier))/(stability.whiteFrontier+stability.blackFrontier);
        return stableScore*0.4  + volatileScore * 0.4 + frontierScore * 0.1;
    }

    private double evaluateBoard(OthelloBoard board, int player) {
        // TODO: maybe randomize the scales?
        return stabilityHeuristics(board, player) * 0.6  + weightedCheckerCountHeuristic(board, player) * 1.0 + movesAvailableHeuristic(board, player) * 0.2;
    }

    private boolean preCheck(OthelloBoard b, OthelloMove m, int player){
        //return true;
        if (b.board[0][0] == 0) {
            if ((m.row == 0 && m.col == 1) || (m.row == 1 && m.col == 0) || (m.row == 1 && m.col == 1)) {
                return false;
            }
        }
        if (b.board[0][b.size-1] == 0) {
            if ((m.row == 0 && m.col == b.size - 2) || (m.row == 1 && m.col == b.size - 2) || (m.row == 1 && m.col == b.size - 1)) {
                return false;
            }
        }
        if (b.board[b.size-1][0] == 0) {
            if ((m.row == b.size - 2 && m.col == 0) || (m.row == b.size - 2 && m.col == 1) || (m.row == b.size - 1 && m.col == 1)) {
                return false;
            }
        }
        if (b.board[b.size-1][b.size-1] == 0) {
            if((m.row == b.size-1 && m.col == b.size-2) || (m.row == b.size-2 && m.col == b.size-2) || (m.row == b.size-2 && m.col == b.size-1)){
                return false;
            }
        }
        return true;
    }

    private OthelloMove minimaxPreferred = null;
    private double minimax(OthelloBoard board, int depth, int player, double cutoffW, double cutoffB) {
        if (depth >= MIN_MAX_DEPTH) {
            return evaluateBoard(board, player);
        }

        double newCutoffW = cutoffW;
        double newCutoffB = cutoffB;
        ArrayList<Double> values = new ArrayList<Double>();

        ArrayList<OthelloMove> legalMoves = board.legalMoves(player);
        if (legalMoves.isEmpty()) {
            return evaluateBoard(board, player);
        }
        for (OthelloMove move : legalMoves) {
            if (!preCheck(board, move, player)) {
                values.add(player == WHITE ? Double.MAX_VALUE : -Double.MAX_VALUE);
                continue;
            }
            OthelloBoard newBoard = cloneBoard(board);
            newBoard.addPiece(move);

            double val = minimax(newBoard, depth+1, flipPlayer(player), newCutoffW, newCutoffB);
            values.add(val);
            if (player == WHITE) {
                if (val < newCutoffB) newCutoffB = val;
                if (val > cutoffB)
                    break;
            } else {
                if (val > newCutoffW) newCutoffW = val;
                if (val < cutoffW)
                    break;
            }
        }
        if (values.isEmpty()) {
            minimaxPreferred = legalMoves.get(0);
            return evaluateBoard(board, player);
        }

        if (player == WHITE) {
            double minValue = Double.MAX_VALUE;
            int minIndex = 0;
            for (int i = 0; i < values.size(); ++i) {
                if (minValue > values.get(i)) {
                    minValue = values.get(i);
                    minIndex = i;
                }
            }
            if (depth == 0) {
                minimaxPreferred = legalMoves.get(minIndex);
            }
            return minValue;
        } else {
            double maxValue = -Double.MAX_VALUE;
            int maxIndex = 0;
            for (int i = 0; i < values.size(); ++i) {
                if (maxValue < values.get(i)) {
                    maxValue = values.get(i);
                    maxIndex = i;
                }
            }
            if (depth == 0) {
                minimaxPreferred = legalMoves.get(maxIndex);
            }
            return maxValue;
        }
    }

    public OthelloMove makeMove(OthelloBoard b) {
        if(b.isLegalMove(new OthelloMove(0,0, playerColor))){
            return new OthelloMove(0,0, playerColor);
        }
        if(b.isLegalMove(new OthelloMove(b.size-1,0, playerColor))){
            return new OthelloMove(b.size-1,0, playerColor);
        }
        if(b.isLegalMove(new OthelloMove(b.size-1,b.size-1, playerColor))){
            return new OthelloMove(b.size-1,b.size-1, playerColor);
        }
        if(b.isLegalMove(new OthelloMove(0,b.size-1, playerColor))){
            return new OthelloMove(0,b.size-1, playerColor);
        }
        if (checkersWeights == null)
            prepareWeights(b.size);
        minimax(b, 0, playerColor, -Double.MAX_VALUE, Double.MAX_VALUE);
        return minimaxPreferred;
    }
}
	