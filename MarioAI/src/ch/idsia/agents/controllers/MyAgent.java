
package ch.idsia.agents.controllers;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;


public class MyAgent extends BasicMarioAIAgent implements Agent
{

	public MyAgent()
	{
		super("MyAgent");
		reset();
	}

	// Does (row, col) contain an enemy?   
	public boolean hasEnemy(int col, int row) {
		return enemies[row][col] != 0;
	}

	// Is (row, col) empty?   
	public boolean isEmpty(int col, int row) {
		return (levelScene[row][col] == 0 || levelScene[row][col] == 2);
	}

	private int getMarioPosInMergedObservation() {
		return mergedObservation.length/2;
	}

	// alias for getMarioPosInMergedObservation()
	private int mPos() {
		return getMarioPosInMergedObservation();
	}

	private int jumpStepsLeft = 0;
	private int scheduledJump = 0;
	private boolean scheduledJumpReady = true;
	private void initiateJump(int length) {
		// scheduling the jump in case of keeping the jump key pressed whiel on the ground
		if (action[Mario.KEY_JUMP] || !isMarioOnGround) {
			scheduledJump = length;
			jumpStepsLeft = 0;
			scheduledJumpReady = false;
		} else {
			scheduledJump = 0;
			jumpStepsLeft = length;
		}

	}

	private void cancelJump() {
		scheduledJump = 0;
		jumpStepsLeft = 0;
	}

	private void handleJump() {
		if (!scheduledJumpReady) {
			action[Mario.KEY_JUMP] = false;
			scheduledJumpReady = true;
		} else {
			// performed the scheduled jump
			if (scheduledJumpReady && scheduledJump != 0) {
				jumpStepsLeft = scheduledJump;
				scheduledJump = 0;
			}
			scheduledJumpReady = true;

			if (jumpStepsLeft > 0) {
				action[Mario.KEY_JUMP] = true;
				jumpStepsLeft--;
			} else {
				action[Mario.KEY_JUMP] = false;
			}
		}
	}

	private class gapInfo {
		public int gapWidth;
		public int gapDepth;
		public int distanceToGap;

		public gapInfo(int width, int depth, int distance) {
			gapWidth = width;
			gapDepth = depth;
			distanceToGap = distance;
		}
	}

	// returns data about a gap closeby if it exists
	private gapInfo calculateGap(boolean toRight) {
		boolean found = false;
		int maxDepth = 0;
		int distanceToGap = 0;
		int gapWidth = 0;

		for (int i = 1; i <= 3 || (found && i < mergedObservation.length/2); ++i) {
			if (isEmpty(mPos()+(toRight ? i : -i), mPos()+1)) {
				if (!found) {
					distanceToGap = i;
				}
				found = true;
				gapWidth++;
				int depth = 0;
				for (;depth < mergedObservation.length/2; ++depth) {
					if (!isEmpty(mPos()+(toRight ? i : -i), mPos()+depth))
						break;
				}
				if (depth == mergedObservation.length/2)
					depth = 1024; // 1024 signifies a gap without a bottom
				if (maxDepth < depth)
					maxDepth = depth;
			}
			else {
				if (found) {
					gapWidth = i - distanceToGap;
					break;
				}
			}
		}
		if (found) {
			return new gapInfo(gapWidth, maxDepth, distanceToGap);
		} else {
			return null;
		}
	}

	boolean sprint = true;
	boolean fireScheduled = false;
	private void fire() {
		fireScheduled = true;
	}
	private void handleFireAndSprint() {
		if (fireScheduled) {
			if (action[Mario.KEY_SPEED])
				action[Mario.KEY_SPEED] = false;
			else {
				action[Mario.KEY_SPEED] = true;
				fireScheduled = false;
			}
		} else {
			action[Mario.KEY_SPEED] = sprint;
		}
	}

	private void landOnEnemy(int maxDistance, int maxDepth) {
		// default: maxDistance = 3;
		// default: maxDepth = 6;
		for (int i = 0; i <= maxDistance && i < mergedObservation.length/2; ++i) {
			for (int j = 1; j <= maxDepth && j < mergedObservation.length/2; ++j) {
				if (!isEmpty(mPos()+i, mPos()+j)) {
					// this field is not empty, skip this column
					continue;
				}
				if (!isEmpty(mPos()-i, mPos()+j)) {
					// this field is not empty, skip this column
					continue;
				}
				if (hasEnemy(mPos()+i, mPos()+j)) {
					if (i == 0)
						setDirection(0);
					else if (j < 2)
						// if you are too close to the enemy, run
						setDirection(-1);
					else
						// otherwise, try to stomp
						setDirection(1);
					return;
				}
				if (hasEnemy(mPos()-i, mPos()+j)) {
					if (i == 0)
						setDirection(0);
					else if (j < 2)
						// if you are too close to the enemy, run
						setDirection(1);
					else
						// otherwise, try to stomp
						setDirection(-1);
					return;
				}
			}

		}
	}

	private int detectEnemiesOnMarioPlatform(int maxDistance, boolean toRight) {
		for (int i = 1; i <= maxDistance && i < mergedObservation.length/2; ++i) {
			if (isEmpty(mPos()+(toRight?i:-i), mPos()+1)) {
				// platform has ended
				return 0;
			}
			if (hasEnemy(mPos()+(toRight?i:-i), mPos())) {
				return i;
			}
		}
		return 0;
	}

	private boolean detectEnemiesAheadUpwards(int maxDistance, int maxHeight, boolean toRight) {
		for (int i = 1; i <= maxHeight && i < mergedObservation.length/2; ++i) {
			for (int j = 1; j <= maxDistance && j < mergedObservation.length/2; ++j) {
				if (!isEmpty(mPos()+(toRight?j:-j), mPos()-i)) {
					// this field is not empty, skip this column
					continue;
				}
				if (hasEnemy(mPos()+(toRight?j:-j), mPos()-i)) {
					return true;
				}
			}

		}
		return false;
	}

	private boolean detectEnemiesAheadDownward(int maxDistance, int maxDepth, boolean toRight) {
		for (int i = 1; i <= maxDistance && i < mergedObservation.length/2; ++i) {
			for (int j = 0; j <= maxDepth && j < mergedObservation.length/2; ++j) {
				if (!isEmpty(mPos()+(toRight?i:-i), mPos()+j)) {
					// this field is not empty, skip this column
					continue;
				}
				if (hasEnemy(mPos()+(toRight?i:-i), mPos()+j)) {
					return true;
				}
			}

		}
		return false;
	}

	private class ObstacleData {
		public int distanceToObstacle;
		public int height;
		public int distanceToEnemy;

		public ObstacleData(int distanceToObstacle, int height, int distanceToEnemy) {
			this.distanceToObstacle = distanceToObstacle;
			this.height = height;
			this.distanceToEnemy = distanceToEnemy;
		}
	}
	// return data about an upcoming obstacle
	private ObstacleData calculateObstacle(int maxDistance, boolean toRight) {
		int maxHeight = 0;
		boolean found = false;
		int distanceToEnemy = 0;
		int distanceToObstacle = 0;
		for (int i = 1; i <= maxDistance && i < mergedObservation.length/2; ++i) {
			if (!isEmpty(mPos()+ (toRight ? i : -i), mPos())) {
				if (!found)
					distanceToObstacle = i;
				found = true;
				int height = 1;
				for (;height < mergedObservation.length/2; ++height) {
					if (isEmpty(mPos()+(toRight ? i : -i), mPos()-height)) {
						if (maxHeight < height)
							maxHeight = height;
						if (distanceToEnemy == 0 && hasEnemy(mPos()+(toRight ? i : -i), mPos()-height))
							distanceToEnemy = i - distanceToObstacle;
						break;
					}
				}
			}
		}
		if (found)
			return new ObstacleData(distanceToObstacle, maxHeight, distanceToEnemy);
		else
			return null;
	}

	private void setDirection(int dir) {
		switch (dir) {
			case 1:
				action[Mario.KEY_RIGHT] = true;
				action[Mario.KEY_LEFT] = false;
				break;
			case -1:
				action[Mario.KEY_RIGHT] = false;
				action[Mario.KEY_LEFT] = true;
				break;
			case 0:
				action[Mario.KEY_RIGHT] = false;
				action[Mario.KEY_LEFT] = false;
				break;
		}
	}
	// Display Mario's view of the world
	public void printObservation() {
		System.out.println("**********OBSERVATIONS**************");
		for (int i = 0; i < mergedObservation.length; i++) {
			for (int j = 0; j < mergedObservation[0].length; j++) {
				if (i == mergedObservation.length / 2 && j == mergedObservation.length / 2) {
					System.out.print("M ");
				}
				else if (hasEnemy(j, i)) {
					System.out.print("E ");
				}
				else if (!isEmpty(j,i)) {

					System.out.print(levelScene[j][i]);
				}
				else {
					System.out.print(" ");
				}
			}
			System.out.println();
		}
		System.out.println("************************");
	}

	public boolean[] getAction2() {
		setDirection(1);

		if (isEmpty(mPos()+1,mPos()+1) || hasEnemy(mPos()+1,mPos()) || (!isEmpty(mPos()+1, mPos())) ||
				isEmpty(mPos()+2,mPos()+1) || hasEnemy(mPos()+2,mPos()) || (!isEmpty(mPos()+2, mPos()))) {
			if (isMarioOnGround)
				initiateJump(8);
			fire();
		}


		/*
		if (isMarioOnGround) {
			gapInfo gap = calculateGap(true);
			enemyDistance = detectEnemiesOnMarioPlatform(7, false);
			ObstacleData obstacle = calculateObstacle(7, true);


		} else {
			landOnEnemy(2,5);
		}
		*/

		handleFireAndSprint();
		handleJump();

		sprint = true;

		//printObservation();
		return action;
	}

	// Actually perform an action by setting a slot in the action array to be true
	public boolean[] getAction()
	{
		if (isMarioOnGround) {
			setDirection(1);
			gapInfo gap = calculateGap(true);
			if (gap != null) {
				System.out.print("Gap"+gap.distanceToGap);
				if (detectEnemiesAheadDownward(5, 10, true)) {
					fire();
					System.out.print("e");
				} else {
					setDirection(1);
				}
				if (gap.distanceToGap <= 2)
					if (gap.gapWidth > 4)
						initiateJump(10);
					else
						initiateJump(6);
				System.out.print("\n");
			}

			int enemyDistance = detectEnemiesOnMarioPlatform(7, true);
			if (enemyDistance != 0) {
				fire();
				initiateJump(10);
				setDirection(1);

			} else {
				enemyDistance = detectEnemiesOnMarioPlatform(7, false);
				if (enemyDistance != 0) {
					initiateJump(10);
				}
			}
			ObstacleData obstacle = calculateObstacle(7, true);
			if (obstacle != null && obstacle.distanceToObstacle <= 1) {
				System.out.println("obstacle" + obstacle.height);
				if (obstacle.height > 0) {
					if (obstacle.height > 5)
						initiateJump(10);
					else if (obstacle.height > 3)
						initiateJump(5);
					else
						initiateJump(3);
				}
			}
		}
		else {
			setDirection(1);
			landOnEnemy(2, 3);
			if (detectEnemiesAheadDownward(10, 10, true)) {
				if (isMarioOnGround && !detectEnemiesAheadDownward(5, 10, true) && !detectEnemiesAheadUpwards(5, 10, true)) {
					setDirection(0);
				}

				fire();
			}
		}


//		if (detectEnemiesAheadUpwards(1, 5, true) || detectEnemiesAheadUpwards(1, 5, false)) {
//			System.out.println("CANCEL");
//			cancelJump();
//			fire();
//		}


		handleFireAndSprint();
		handleJump();

		sprint = false;

		//printObservation();
		return action;
	}

	// Do the processing necessary to make decisions in getAction
	public void integrateObservation(Environment environment)
	{
		super.integrateObservation(environment);
		levelScene = environment.getLevelSceneObservationZ(2);
	}

	// Clear out old actions by creating a new action array
	public void reset()
	{
		action = new boolean[Environment.numberOfKeys];
	}
}
