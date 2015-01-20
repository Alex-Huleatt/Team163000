package team163000.units;

import java.util.Random;

import team163000.CHANNELS;
import team163000.Constants;
import team163000.moving.BasicBugger;
import team163000.moving.PathMove;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Miner implements Unit {

	private RobotController rc;
	private Team myTeam;
	private Team enemyTeam;
	private int myRange;
	private Random rand;
	private MapLocation bestLoc;
	private int lifetime = 0;
	
	private PathMove pathMove;
	private MapLocation targetLoc;
	public BasicBugger bb;
	private State state;
	private Direction confusedDirection;

	public static final boolean TESTING_MINING = false;
	public static final int ORE_CHANNEL = 10000;
	public static final int SUPPLY_THRESHOLD = 500;
	
	public int resupplyChannel = 0;
	public RobotInfo[] enemies;
	public double myHealth;
	public MapLocation myLoc;
	public int oreHere;
	public int bestVal;
	
	public void onSpawn(RobotController rc) {
		this.rc = rc;
		rand = new Random(rc.getID());
		myRange = rc.getType().attackRadiusSquared;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myHealth = rc.getHealth();
		state = new Mining();
		pathMove = new PathMove(rc);
		bb = new BasicBugger(rc);
		confusedDirection = Constants.directions[rand.nextInt(8)];
	}

	public void run() {
		while (true) {
			try {
				lifetime++;
				myLoc = rc.getLocation();
				oreHere = (int) (rc.senseOre(myLoc) + .5);
				bestVal = rc.readBroadcast(1000);
				bestLoc = new MapLocation(rc.readBroadcast(1001), rc.readBroadcast(1002));
				enemies = rc.senseNearbyRobots(24, enemyTeam);

				// Update the most lucrative position's ore
				if (myLoc.x == rc.readBroadcast(1001) && myLoc.y == rc.readBroadcast(1002)) {
					rc.broadcast(1000, (int) (rc.senseOre(myLoc) + .5));
				}

				// Run the current state
				state = state.run(rc, this);

				if (TESTING_MINING && (Clock.getRoundNum() == 1000 || Clock.getRoundNum() == 1999)) {
					System.out.println("Ore Extracted: " + rc.readBroadcast(ORE_CHANNEL));
				}

				rc.yield();
			} catch (Exception e) {
				System.out.println("Miner Exception");
				e.printStackTrace();
			}
		}
	}

	private boolean goingToBest;

	public void defaultMove() throws GameActionException {

		// Logic to decongest and remove walls of people
		RobotInfo[] friends = rc.senseNearbyRobots(myRange, myTeam);
		for (int i = 0; i < friends.length; i++) {
			RobotInfo friend = friends[i];

			if (friend.type == RobotType.MINER && rc.senseOre(friend.location) < 3 && rc.senseOre(myLoc.add(friend.location.directionTo(myLoc))) > 3) {
				Constants.tryMove(rc,friend.location.directionTo(myLoc));
			}
		}

		if (rc.isCoreReady() && rc.canMine() && oreHere > 1) {
			rc.setIndicatorString(1, "Mining");
			MineHere();
		} else {
			if (bestVal > oreHere + 2.5 * Math.sqrt(myLoc.distanceSquaredTo(bestLoc) * RobotType.MINER.movementDelay)) {
				if (!bestLoc.equals(bb.goal)) {
					bb.setDestination(bestLoc);
				}
				bb.attemptMove();
				return;
			}
			Direction d = findSpot();
			if (d == Direction.NONE) {
				rc.setIndicatorString(1, "Moving to best: " + bestLoc);
				if (rc.readBroadcast(1000) < 3) {
					Constants.tryMove(rc,confusedDirection);
				} else {
					if (!bestLoc.equals(bb.goal)) {
						bb.setDestination(bestLoc);
					}
					bb.attemptMove();
				}
			} else if (rc.isCoreReady() && rc.canMove(d)) {
				rc.setIndicatorString(1, "Moving nearby: " + d);
				Constants.tryMove(rc,d);
			}
		}
	}

	private Direction findSpot() throws GameActionException {
		double bestFound = 3;
		Direction[] counts = new Direction[9];
		counts[0] = Direction.NONE;
		int count = 1;

		for (int i = 0; i < 8; i++) {
			double oreHere = rc.senseOre(myLoc.add(Constants.directions[i]));
			if (bestFound < oreHere && rc.canMove(Constants.directions[i])) {
				count = 1;

				counts[0] = Constants.directions[i];
				bestFound = oreHere;
			} else if (bestFound == oreHere) {
				counts[count++] = Constants.directions[i];
			}
		}

		return counts[rand.nextInt(count)];
	}

	public void MineHere() throws GameActionException {
		if (TESTING_MINING) {
			int extracted = (int) (Math.max(Math.min(3, oreHere / 4), 0.2) * 10);
			rc.broadcast(ORE_CHANNEL, rc.readBroadcast(ORE_CHANNEL) + extracted);
		}

		rc.mine();
	}
}

interface State {
	State run(RobotController rc, Miner m) throws GameActionException;
}

//State for when the miner is just mining along
class Mining implements State {

	public State run(RobotController rc, Miner m) throws GameActionException {
		rc.setIndicatorString(0, "Mining");

		// Check our supply level, and put in a request
		if (rc.getSupplyLevel() < Miner.SUPPLY_THRESHOLD && this.requestSupply(rc,m)) {
			return new Resupplying();
		} else if (rc.getSupplyLevel() > Miner.SUPPLY_THRESHOLD) {
			m.resupplyChannel = 0;
		}

		//Check if we need to start retreating
		double curHealth = rc.getHealth();
		if (m.enemies.length > 0 || curHealth < m.myHealth) {
			m.myHealth = curHealth;

			//Broadcast out our position
			rc.broadcast(CHANNELS.PANIC_X.getValue(), m.myLoc.x);
			rc.broadcast(CHANNELS.PANIC_Y.getValue(), m.myLoc.y);

			State retreat = new Retreating();
			retreat.run(rc, m);

			return retreat;
		}

		//Otherwise just default move
		if (rc.isCoreReady()) {
			if (m.oreHere > m.bestVal) {
				rc.broadcast(1000, m.oreHere);
				rc.broadcast(1001, m.myLoc.x);
				rc.broadcast(1002, m.myLoc.y);
			}
			m.defaultMove();
		}

		return this;
	}

	boolean requestSupply(RobotController rc, Miner m) throws GameActionException {
		m.resupplyChannel = SupplyDrone.requestResupply(rc, rc.getLocation(), m.resupplyChannel);

		int head = rc.readBroadcast(196);
		MapLocation beaverLoc = new MapLocation(rc.readBroadcast(198), rc.readBroadcast(199));

		//If we are close, and are the resupply target, than trigger a resupply
		return head == m.resupplyChannel && beaverLoc.distanceSquaredTo(rc.getLocation()) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED + 5;
	}
}

//State for when the miner is close to a beaver, and awaiting a resupply
class Resupplying implements State {

	int roundsWaited = 0;

	public State run(RobotController rc, Miner m) throws GameActionException {
		rc.setIndicatorString(0, "Resupplying on channel " + m.resupplyChannel);

		//Check if we need to start retreating
		double curHealth = rc.getHealth();
		if (m.enemies.length > 0 || curHealth < m.myHealth) {
			State retreat = new Retreating();
			retreat.run(rc, m);

			return retreat;
		}

		//Just relax and mine until we are resupplied
		if (rc.isCoreReady() && rc.canMine() && m.oreHere > 0) {
			m.MineHere();
		}

		//If the resupply hasn't come yet, it probably isnt' coming so just go back to mining
		roundsWaited++;
		if (roundsWaited == 10) {
			return new Mining();
		}

		//Transition to Mining once we are resupplied
		if (rc.getSupplyLevel() > Miner.SUPPLY_THRESHOLD) {
			return new Mining();
		}

		return this;
	}
}

//State for when we are running away!
class Retreating implements State {

	int roundsSinceEnemy = 0;

	public State run(RobotController rc, Miner m) throws GameActionException {
		rc.setIndicatorString(0, "Retreating");

		//Move towards the HQ
		MapLocation hq = rc.senseHQLocation();
		if (!hq.equals(m.bb.goal)) {
			m.bb.setDestination(hq);
		}
		m.bb.attemptMove();

		//send panic on being attacked
		double curHealth = rc.getHealth();
		if (curHealth < m.myHealth) {
			m.myHealth = curHealth;

			//Broadcast out our position
			rc.broadcast(911, m.myLoc.x);
			rc.broadcast(912, m.myLoc.y);
			return this;
		}

		//run from enemies
		if (m.enemies.length == 0) {
			roundsSinceEnemy++;
		}

		//We are probably safe now
		if (roundsSinceEnemy == 5) {
			return new Mining();
		}

		return this;
	}
}
