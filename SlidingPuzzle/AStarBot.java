import java.util.*;

/**
 * Created by mike on 8/31/16.
 */

class AStarBot extends SlidingPlayer {
    private class HashedBoard {
        public int[][] board;
        @Override
        public int hashCode() {
            return Arrays.deepHashCode(board);
        }

        @Override
        public boolean equals(Object otherObject) {
            int[][] compareWith = null;
            if (otherObject instanceof HashedBoard)
                compareWith = ((HashedBoard) otherObject).board;
            else
                return false;
            return Arrays.deepEquals(board, compareWith);
        }

        public HashedBoard(SlidingBoard board) {
            this.board = new int[board.size][board.size];
            for (int x = 0; x < board.size; ++x) {
                for (int y = 0; y < board.size; ++y)
                    this.board[x][y] = board.board[x][y];
            }
        }
    }

    // an internal class storing the game states
    // It implements Comparable, thanks to which it can be used in a priority queue
    private class gameState implements Comparable{
        public SlidingBoard board;
        public ArrayList<SlidingMove> moves;
        public int depth;
        public double cost;

        public gameState(SlidingBoard board, ArrayList<SlidingMove> previousMoves, SlidingMove move, int depth) {
            this.board = board;
            this.moves = (ArrayList<SlidingMove>) previousMoves.clone();
            if (move != null) {
                this.moves.add(move);
                this.board.doMove(move);
            }
            // The first part of the cost is the actual number of moves
            this.depth = depth;
            this.cost = depth;

            // Set up the remaining cost to manhattan distances
            if (!board.isSolved()) {
                for (int x = 0; x < board.size; ++x)
                    for (int y = 0; y < board.size; ++y) {
                        int expectedX, expectedY;
                        expectedX = board.board[y][x] % board.size;
                        expectedY = board.board[y][x] / board.size;
                        this.cost += Math.abs(x - expectedX) + Math.abs(y - expectedY);
                    }
                this.cost -= 1.0;
            }


        }

        // Needed for Comparable
        @Override
        public int compareTo(Object otherNode) {
            gameState other = (gameState) otherNode;
            return new Double(cost).compareTo(new Double(other.cost));
        }
    }

    public SlidingBoard cloneBoard(SlidingBoard board) {
        return new SlidingBoard(board);
    }

    // Main method of A-Star
    public ArrayList<SlidingMove> findPath(SlidingBoard _sb) {
        // For optimization, we will avoid boards that have already happened
        HashSet<HashedBoard> usedMoves = new HashSet<HashedBoard>();
        gameState current = new gameState(_sb, new ArrayList<SlidingMove>(), null, 0);

        // Frontier contains all the possible states which can lead to the solution
        PriorityQueue<gameState> frontier = new PriorityQueue<>();

        // Until we find the solution:
        while (!current.board.isSolved()) {
            ArrayList<SlidingMove> legalMoves = current.board.getLegalMoves();
            for (SlidingMove m : legalMoves) {
                // Make a new gameState, which will also calculate its cost, and put it in the right
                // spot in the prioritized queue
                gameState newState = new gameState(cloneBoard(current.board), current.moves, m, current.depth+1);

                // Make sure states do not repeat
                if (!usedMoves.contains(new HashedBoard(newState.board))) {
                    usedMoves.add(new HashedBoard(newState.board));
                    frontier.add(newState);
                }
            }
            // get the next game state to analyze
            current = frontier.poll();
        }

        return current.moves;
    }

    private ArrayList<SlidingMove> movesToPerform;
    // The constructor gets the initial board
    public AStarBot(SlidingBoard _sb) {
        super(_sb);
        nextMove = 0;
        SlidingBoard copy = new SlidingBoard(_sb.size);
        copy.setBoard(_sb);
        movesToPerform = findPath(copy);
        int a = 1;
    }

    private int nextMove;
    // Perform a single move based on the current given board state
    public SlidingMove makeMove(SlidingBoard board) {
        Random r = new Random();

        //ArrayList<SlidingMove> legalMoves = board.getLegalMoves();
        nextMove++;
        return movesToPerform.get(nextMove-1);
    }
}