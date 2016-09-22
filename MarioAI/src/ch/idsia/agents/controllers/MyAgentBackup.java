
package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import java.util.ArrayList;


class Coordinate {
	int row;
	int col;

	public Coordinate(int _row, int _col) {
		row = _row;
		col = _col;
	}

	public int getX() {
		return col;
	}

	public int getY() {
		return row;
	}

	public Coordinate moveUp() {
		return new Coordinate(this.row - 1, this.col);
	}

	public Coordinate moveDown() {
		return new Coordinate(this.row + 1, this.col);
	}

	public Coordinate moveLeft() {
		return new Coordinate(this.row, this.col-1);
	}

	public Coordinate moveRight() {
		return new Coordinate(this.row, this.col+1);
	}

	public boolean equals(Coordinate other) {
		return (this.row == other.getY() && this.col == other.getX());
	}

	public boolean isAbove(Coordinate other) {
		return (this.row < other.row);
	}
	public boolean isRightOf(Coordinate other) {
		return (this.col > other.col);
	}

	public String toString() {
		return "(" + row + ", " + col + ")";
	}
}

class Path {
	ArrayList<Coordinate> path;

	public Path() {
		path = new ArrayList<Coordinate>();
	}

	public void addStep(Coordinate c) {
		path.add(c);
	}

	public boolean contains(Coordinate c) {
		for (int i = 0; i < path.size(); i++ ) {
			if (c.equals(path.get(i))) {
				return true;
			}
		}
		return false;
	}

	public int size() {
		return path.size();
	}

	public Path getCopy() {
		Path result = new Path();
		for (int i = 0; i < path.size(); i++ ) {
			result.addStep(path.get(i));
		}
		return result;
	}
	
	public String toString() {
		String result = "PATH\n\n";
		for (int i = 0; i < path.size(); i++ ) {
			result += path.get(i) + " ";
		}
		result += "\n\nENDPATH\n\n";
		return result;
	}
}


public class MyAgent extends BasicMarioAIAgent implements Agent
{

	public MyAgent()
	{
		super("MyAgent");
		reset();
	}

	public char getType(int v) {
		if (v == -60) return 'B';
		else if (v == -24) return 'B';
		else if (v == 20) return 'C';
		else if (v >= 1 && v <= 19) return 'E';
        else return '0';
	}

	public double[][] getScores() {
		double[][] scores = new double[mergedObservation.length][mergedObservation[0].length];

		// squares on the right are good by default
		for (int i = 0; i < mergedObservation.length; i++) {
			for (int j = 0; j < mergedObservation[0].length; j++) {
				//System.out.println(i + " " + j);
				scores[i][j] = (25 - i) + 4 * j;
			}
		}

		// enemies are bad
		for (int i = 0; i < mergedObservation.length; i++) {
			for (int j = 0; j < mergedObservation[0].length; j++) {
				//System.out.println(i + " " + j);
				if (hasEnemy(mergedObservation[i][j])) {
					scores[i][j] -= 250;
				}
			}
		}

		// bricks are kinda bad
		for (int i = 0; i < mergedObservation.length; i++) {
			for (int j = 0; j < mergedObservation[0].length; j++) {
				//System.out.println(i + " " + j);
				if (isBrick(mergedObservation[i][j])) {
					scores[i][j] -= 50;
				}
			}
		}

		return scores;
 	}

	public ArrayList<Coordinate> getPossibleMovesGreedy(double[][] scores, Coordinate currPosition) {
		ArrayList<Coordinate> bestMoves = new ArrayList<Coordinate>();
		Coordinate best = null;
		double bestScore = -100000;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				if (currPosition.getX() + i >= 0 && currPosition.getX() + i <= 18 && currPosition.getY() + j >= 0 && currPosition.getY() + j <= 18) {
					//System.out.println((currPosition.getX() + i) + " " + (currPosition.getY() + j));
					if (scores[currPosition.getX() + i][currPosition.getY() + j] > bestScore) {
						bestScore = scores[currPosition.getX() + i][currPosition.getY() + j];
						best = new Coordinate(currPosition.getX() + i, currPosition.getY() + j);
					}
				}
			}
		}

		//bestMoves.add(best);
		bestMoves.add(new Coordinate(currPosition.getX() + 1, currPosition.getY()));
		//bestMoves.add(new Coordinate(currPosition.getX() + 1, currPosition.getY()+1));
		//bestMoves.add(new Coordinate(currPosition.getX() + 1, currPosition.getY()-1));
		return bestMoves;
	}

	public Path findPathHelper(double[][] scores, Coordinate currPosition, Path currentPath) {
		Path result = currentPath.getCopy();
		result.addStep(currPosition);
		if (currPosition.getX() >= 18) {
			return result;
		}
		
		else {
			ArrayList<Coordinate> moves = getPossibleMovesGreedy(scores, currPosition);
			for (Coordinate move : moves) {
				//System.out.println(move);
				if (!currentPath.contains(move)) {
					Path newPath = findPathHelper(scores, move, result);
					if (newPath != null) {
						return newPath;
					}
				}
			}

			return null;
		}
	}

	public Path findPath(double[][] scores) {
		return findPathHelper(scores, new Coordinate(11, 11), new Path());
	}

	public boolean hasEnemy(int v) {
		return (v >= 1 && v <= 19);
	}

	public boolean isBrick(int v) {
		return (v == -60);
	}

	public void printObservation() {
		System.out.println("**********LEVEL**************");
		/*for (int i = START_ZOOM; i < END_ZOOM; i++) {
			for (int j = START_ZOOM; j < END_ZOOM; j++) {
				//System.out.print(String.format("%4c", getType(levelScene[i][j])));
				//System.out.print(getType(levelScene[i][j]) + " ");
				System.out.print(levelScene[i][j] + " ");
			}
			System.out.println();
		}*/
		System.out.println("*********ENEMIES***************");
		/*for (int i = START_ZOOM; i < END_ZOOM; i++) {
			for (int j = START_ZOOM; j < END_ZOOM; j++) {
				//System.out.print(String.format("%4c", getType(levelScene[i][j])));
				//System.out.print(getType(levelScene[i][j]) + " ");
				System.out.print(enemies[i][j] + " ");
			}
			System.out.println();
		}*/	
		System.out.println("************************");
		System.out.println("*********SCORES***************");
		double[][] scores = getScores();
		for (int i = 0; i < levelScene.length; i++) {
			for (int j = 0; j < levelScene[0].length; j++) {
				//System.out.print(String.format("%4c", getType(levelScene[i][j])));
				//System.out.print(getType(levelScene[i][j]) + " ");
				System.out.print(scores[i][j] + " ");
			}
			System.out.println();
		}
		System.out.println("************************");

		System.out.println("***********SCORES*************");
		System.out.println(findPath(scores));
		System.out.println("************************");
		System.exit(1);

	}

	public boolean[] getAction()
	{
		action[Mario.KEY_SPEED] = action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
        printObservation();
		return action;
	}

	public void integrateObservation(Environment environment)
	{
		super.integrateObservation(environment);
    	levelScene = environment.getLevelSceneObservationZ(2);
	}

	public void reset()
	{
		action = new boolean[Environment.numberOfKeys];
		action[Mario.KEY_RIGHT] = true;
		action[Mario.KEY_SPEED] = true;
	}
}
