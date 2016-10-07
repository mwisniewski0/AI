import jdk.nashorn.internal.runtime.regexp.joni.SearchAlgorithm;

import java.util.*;

class MyBot extends SlidingPlayer {
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


/*    // Recursive version
    public ArrayList<SlidingMove> dfs(SlidingBoard _sb, ArrayList<SlidingMove> result, HashSet<HashedBoard> usedMoves) {
        if (result == null)
           result = new ArrayList<SlidingMove>();
        if (usedMoves == null) {
            usedMoves = new HashSet<HashedBoard>();
        }

        if (usedMoves.contains(new HashedBoard(_sb)))
            return null;

        if (_sb.isSolved())
            return result;

        ArrayList<SlidingMove> legalMoves = _sb.getLegalMoves();

        usedMoves.add(new HashedBoard(_sb));
        for (SlidingMove move : legalMoves) {
            result.add(move);

            int dir = _sb.doMove(move);
            ArrayList<SlidingMove> newResult = dfs(_sb, result, usedMoves);
            if (newResult != null)
                return newResult;
            _sb.undoMove(move, dir);

            result.remove(result.size() - 1);
        }
        usedMoves.remove(new HashedBoard(_sb));

        return null;
    }*/

    public SlidingBoard cloneBoard(SlidingBoard board) {
//        SlidingBoard newBoard = new SlidingBoard(board.size);
//        newBoard.setBoard(board);
//        return newBoard;
        return new SlidingBoard(board);
    }

    private class gameState {
        public SlidingBoard board;
        public ArrayList<SlidingMove> moves;
        public int depth;

        public gameState(SlidingBoard board, ArrayList<SlidingMove> previousMoves, SlidingMove move, int depth) {
            this.board = board;
            this.moves = (ArrayList<SlidingMove>) previousMoves.clone();
            if (move != null) {
                this.moves.add(move);
                this.board.doMove(move);
            }
            this.depth = depth;
        }
    }

    public ArrayList<SlidingMove> bfs(SlidingBoard _sb) {
        ArrayList<SlidingMove> result = new ArrayList<SlidingMove>();
        HashSet<HashedBoard> usedMoves = new HashSet<HashedBoard>();

        if (_sb.isSolved())
            return result;

        gameState current = null;
        SlidingBoard currentBoard = _sb;
        Queue<gameState> movesToCheck = new LinkedList<gameState>();
        ArrayList<SlidingMove> legalMoves = _sb.getLegalMoves();

        for (SlidingMove m : legalMoves)
            movesToCheck.add(new gameState(cloneBoard(_sb),new ArrayList<SlidingMove>(), m, 0));

        while (!currentBoard.isSolved()) {
            current = movesToCheck.remove();
            currentBoard = current.board;

            if (usedMoves.contains(new HashedBoard(currentBoard)))
                continue;
            usedMoves.add(new HashedBoard(currentBoard));

            legalMoves = currentBoard.getLegalMoves();
            for (SlidingMove m : legalMoves)
                movesToCheck.add(new gameState(cloneBoard(currentBoard), current.moves, m, current.depth+1));
        }

        if (current == null)
            return new ArrayList<SlidingMove>();
        return current.moves;
    }

    public ArrayList<SlidingMove> iterDFS(SlidingBoard _sb) {
        int depth = 1;
        ArrayList<SlidingMove> result = null;
        while (result == null) {
            result = partDfs(_sb, depth);
            depth++;
        }
        return result;
    }

    public ArrayList<SlidingMove> partDfs(SlidingBoard _sb, int maxDepth) {
        HashSet<HashedBoard> usedMoves = new HashSet<HashedBoard>();
        gameState current = new gameState(_sb, new ArrayList<SlidingMove>(), null, 0);
        LinkedList<gameState> toCheck = new LinkedList<gameState>();
        while (!current.board.isSolved()) {
            ArrayList<SlidingMove> legalMoves = current.board.getLegalMoves();
            for (SlidingMove m : legalMoves) {
                gameState newState = new gameState(cloneBoard(current.board), current.moves, m, current.depth+1);
                if (newState.depth <= maxDepth && !usedMoves.contains(new HashedBoard(newState.board))) {
                    usedMoves.add(new HashedBoard(newState.board));
                    toCheck.push(newState);
                }
            }
            if (toCheck.isEmpty())
                return null;
            current = toCheck.pop();
        }

        return current.moves;
    }

    public ArrayList<SlidingMove> dfs(SlidingBoard _sb) {
        HashSet<HashedBoard> usedMoves = new HashSet<HashedBoard>();
        gameState current = new gameState(_sb, new ArrayList<SlidingMove>(), null, 0);
        LinkedList<gameState> toCheck = new LinkedList<gameState>();
        while (!current.board.isSolved()) {
            ArrayList<SlidingMove> legalMoves = current.board.getLegalMoves();
            for (SlidingMove m : legalMoves) {
                gameState newState = new gameState(cloneBoard(current.board), current.moves, m, current.depth+1);
                if (!usedMoves.contains(new HashedBoard(newState.board))) {
                    usedMoves.add(new HashedBoard(newState.board));
                    toCheck.push(newState);
                }
            }
            current = toCheck.pop();
        }

        return current.moves;
    }

    private ArrayList<SlidingMove> movesToPerform;
    // The constructor gets the initial board
    public MyBot(SlidingBoard _sb) {
        super(_sb);
        nextMove = 0;
        SlidingBoard copy = new SlidingBoard(_sb.size);
        copy.setBoard(_sb);
        movesToPerform = dfs(copy);
        int a = 1;
    }

    private int nextMove;
    // Perform a single move based on the current given board state
    public SlidingMove makeMove(SlidingBoard board) {
        Random r = new Random();
        
        //ArrayList<SlidingMove> legalMoves = board.getLegalMoves();
        nextMove++;
        System.out.println(nextMove);
        return movesToPerform.get(nextMove-1);
    }   
}