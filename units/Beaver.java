package team163000.units;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team163000.Constants;
import team163000.moving.BasicBugger;
import team163000.moving.PathMove;
import team163000.StratController;

public class Beaver implements Unit {

	public RobotController rc;
	public Team myTeam;
	public Team enemyTeam;
	public int myRange;
	public Random rand;
	public int lifetime = 0;
	public MapLocation bestLoc;
	public int bestVal;
	public boolean wasBest = false;
	public int wasBest_count = 0;
	public MapLocation myLoc;
	public int oreHere;
	public double myHealth;
	public RobotInfo[] enemies;
	public BasicBugger bb;

	//pathfinding for building
	public PathMove panther;
	
	public void onSpawn(RobotController rc) {
		this.rc = rc;
		rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myHealth = rc.getHealth();
		// check if saved in memory to go with air attack
		panther = new PathMove(rc);
		bb = new BasicBugger(rc);
	}

	public void run() {
		while (true) {
			try {
				lifetime++;
				myLoc = rc.getLocation();
				if (rc.isWeaponReady()) {
					attackSomething();
				}

//				//send panic on being attacked
//				enemies = rc.senseNearbyRobots(24, enemyTeam);
//				double curHealth = rc.getHealth();
//				if (curHealth < myHealth) {
//					myHealth = curHealth;
//					rc.broadcast(911, myLoc.x);
//					rc.broadcast(912, myLoc.y);
//					Move.tryMove(rc.senseHQLocation());
//					continue;
//				}
//
//				//run from enemies
//				if (enemies.length > 0) {
//					Move.tryMove(rc.senseHQLocation());
//					continue;
//				}

				oreHere = (int) (rc.senseOre(myLoc) + .5);
				bestVal = rc.readBroadcast(1000);
				bestLoc = new MapLocation(rc.readBroadcast(1001),
						rc.readBroadcast(1002));

				buildStuff();
				if (rc.isCoreReady()) {
					if (oreHere > bestVal) {
						rc.broadcast(1000, oreHere);
						rc.broadcast(1001, myLoc.x);
						rc.broadcast(1002, myLoc.y);
					}
					defaultMove();
				}
				rc.yield();
			} catch (Exception e) {
				System.out.println("Beaver Exception");
				e.printStackTrace();
			}
		}
	}

	public boolean isStationary(RobotType rt) {
		return (rt != null && (rt == RobotType.AEROSPACELAB
				|| rt == RobotType.BARRACKS || rt == RobotType.HELIPAD
				|| rt == RobotType.HQ || rt == RobotType.MINERFACTORY
				|| rt == RobotType.SUPPLYDEPOT || rt == RobotType.TANKFACTORY
				|| rt == RobotType.TECHNOLOGYINSTITUTE || rt == RobotType.TOWER || rt == RobotType.TRAININGFIELD));
	}

	public void defaultMove() throws GameActionException {
		if (panther.goal.isAdjacentTo(myLoc)) {
			RobotType toMake = StratController.toMake(rc);
			Direction dir = myLoc.directionTo(panther.goal);
			if (toMake!=null && rc.canBuild(dir, toMake)) {
				rc.build(myLoc.directionTo(panther.goal),toMake);
			}
			
		} else panther.attemptMove();
	}

	public void buildStuff() throws GameActionException {
		if (panther.goal==null || !StratController.shouldBuildHere(rc, panther.goal)) {
			panther.setDestination(StratController.findBuildLocation(rc));
		}
	}

	public int directionToInt(Direction d) {
		switch (d) {
		case NORTH:
			return 0;
		case NORTH_EAST:
			return 1;
		case EAST:
			return 2;
		case SOUTH_EAST:
			return 3;
		case SOUTH:
			return 4;
		case SOUTH_WEST:
			return 5;
		case WEST:
			return 6;
		case NORTH_WEST:
			return 7;
		default:
			return -1;
		}
	}

	// This method will attack an enemy in sight, if there is one
	public void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	// This method will attempt to build in the given direction (or as close to
	// it as possible)
	public boolean tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8
				&& !rc.canMove(Constants.directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8 && rc.isCoreReady()) {
			rc.build(Constants.directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
			return true;
		}
		return false;
	}

}
