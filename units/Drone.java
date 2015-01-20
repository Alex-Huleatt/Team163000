package team163000.units;

import java.util.Random;

import team163000.AttackUtils;
import team163000.CHANNELS;
import team163000.Constants;
import team163000.Supply;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.TerrainTile;

public class Drone implements Unit {
	public Unit subunit;
	public RobotController rc;
	public int range;
	public Team team;
	public Team opponent;
	public MapLocation hq;
	public MapLocation enemyHQ;
	public Random random;
	public State state;
	public RobotInfo[] enemies;
	public RobotInfo[] allies;
	
    static Direction pre = Direction.NORTH;
    static boolean right = true;
    
	public void onSpawn(RobotController rc) {
		Unit sub;
		try {
			sub = makeSubunit(rc);
			if (sub != null) {
				subunit = sub;
				subunit.onSpawn(rc);
			} else {
				this.rc = rc;
				range = rc.getType().attackRadiusSquared;
				team = rc.getTeam();
				opponent = rc.getTeam().opponent();
				hq = rc.senseHQLocation();
				enemyHQ = rc.senseEnemyHQLocation();
				random = new Random(rc.getID());
				state = new Patrolling();
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Unit makeSubunit(RobotController rc) throws GameActionException {
		int round = Clock.getRoundNum();
		int lastRound = round - 1;
		if (round < 500 && rc.readBroadcast(CHANNELS.NUMBER_DRONE.getValue()) < 2) {
			return new ScoutDrone();
		}
		int supplierAlive = rc.readBroadcast(CHANNELS.SUPPLY_DRONE1.getValue());
		if (supplierAlive != round && supplierAlive != lastRound) {
			return new SupplyDrone(1); 
		}
		supplierAlive = rc.readBroadcast(CHANNELS.SUPPLY_DRONE2.getValue());
		if (supplierAlive != round && supplierAlive != lastRound) {
			return new SupplyDrone(2);
		}

		supplierAlive = rc.readBroadcast(CHANNELS.SUPPLY_DRONE3.getValue());
		if (supplierAlive != round && supplierAlive != lastRound) {
			return new SupplyDrone(3);
		}
		return null;
	}

	public void run() {
		if (subunit != null) subunit.run();
		 
         while (true) {
        	 try {
             enemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, opponent);
             allies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, team);
             state = state.run(rc, this);

             rc.yield();
        	 } catch (GameActionException e) {
        		 e.printStackTrace();
        	 }
         }

	}
	
	interface State {

	    State run(RobotController rc, Drone d) throws GameActionException;
	}

	class Patrolling implements State {

	    MapLocation nextTower = null;

	    public State run(RobotController rc, Drone d) throws GameActionException {
	        if (nextTower != null) {
	            rc.setIndicatorString(0, "Patrolling to " + nextTower.toString());
	        }

	        int x = rc.readBroadcast(CHANNELS.PANIC_X.getValue());
	        int y = rc.readBroadcast(CHANNELS.PANIC_Y.getValue());

	        if (x != 0 || y != 0) {
	            nextTower = new MapLocation(x, y);
	        }

	        //add goal location to patrol if attacking
	        if (rc.readBroadcast(CHANNELS.ORDER66.getValue()) == 1
	                && nextTower == null) {
	            x = rc.readBroadcast(CHANNELS.GOAL_X.getValue());
	            y = rc.readBroadcast(CHANNELS.GOAL_Y.getValue());
	            nextTower = new MapLocation(x, y);
	        }

	        if (d.enemies.length > 0
	                && ((double) d.allies.length + 1.0) * 1.3 > d.enemies.length) {
	            Chasing state = new Chasing();
	            //state.run(rc);

	            return state;
	        }

	        if (nextTower == null || rc.getLocation().distanceSquaredTo(nextTower) <= 3) {
	            if (nextTower != null && nextTower.x == x && nextTower.y == y) {
	                rc.broadcast(CHANNELS.PANIC_X.getValue(), 0);
	                rc.broadcast(CHANNELS.PANIC_Y.getValue(), 0);
	            }

	            MapLocation[] towers = rc.senseTowerLocations();
	            int index = (int) (Math.random() * towers.length + 1);

	            //add chance to patroll ore location
	            if (Math.random() > 0.8) {
	                int xOre = rc.readBroadcast(CHANNELS.BEST_ORE_X.getValue());
	                int yOre = rc.readBroadcast(CHANNELS.BEST_ORE_Y.getValue());
	                nextTower = new MapLocation(xOre, yOre);
	            } else {
	                if (index == towers.length) {
	                    nextTower = d.hq;
	                } else {
	                    if (index > towers.length) {
	                        nextTower = d.hq;
	                    } else {
	                        nextTower = towers[index];
	                    }
	                }
	            }
	        }

	        tryKite(nextTower, rc.senseEnemyTowerLocations());

	        if (Clock.getBytecodesLeft() > 500) {
	            Supply.supplyConservatively(rc, d.team);
	        }

	        return this;
	    }
	}

	class Responding implements State {

	    public State run(RobotController rc, Drone d) throws GameActionException {
	        return this;
	    }
	}

	class Chasing implements State {

	    public State run(RobotController rc, Drone d) throws GameActionException {
	        rc.setIndicatorString(0, "Chasing");
	        if (d.enemies.length == 0) {
	            Patrolling state = new Patrolling();
	            //state.run(rc);

	            return state;
	        }

	        AttackUtils.attackSomething(rc, d.range, d.opponent);
	        MapLocation[] towers = rc.senseEnemyTowerLocations();
	        if (d.enemies.length > 0
	                && inTowerRange(d.enemies[0].location, towers)) {
	            //enemy moved into tower range
	            if (d.allies.length < 4) {
	                //call off chase
	                Patrolling state = new Patrolling();
	                //state.run(rc);

	                return state;
	            }
	        }

	        //test if outnumbered
	        if (((double) d.allies.length + 1.0) * 1.3 < d.enemies.length) {
	            Patrolling state = new Patrolling();
	            //state.run(rc);
	            return state;
	        }

	        if (d.enemies.length > 0) {
	            if (d.allies.length > 4) { //charge
	                tryFly(d.enemies[0].location);
	            } else {
	                tryKite(d.enemies[0].location, rc.senseEnemyTowerLocations());
	            }
	        }
	        return this;
	    }
	}
	
    /**
     * Does not take into account walls and trys to kite around towers and stuff
     *
     * @param target map location to go to
     * @param objects stuff to kite around such as tower locations
     */
    public void tryKite(MapLocation target, MapLocation[] objects) {
        try {
            Direction dir = pre;
            MapLocation myLoc = rc.getLocation();
            MapLocation next = myLoc.add(dir);
            if (myLoc.compareTo(target) == 0) {
                target = rc.senseHQLocation();
            }

            if (right && inTowerRange(myLoc.add(pre), objects)
                    && inTowerRange(myLoc.add(pre.rotateRight()), objects)
                    && inTowerRange(myLoc.add(pre.rotateRight().rotateRight()), objects)) {
                right = false;
            }

            if (!right && inTowerRange(myLoc.add(pre), objects)
                    && inTowerRange(myLoc.add(pre.rotateLeft()), objects)
                    && inTowerRange(myLoc.add(pre.rotateLeft().rotateLeft()), objects)) {
                right = true;
            }

            boolean check = true;
            int count = 8;
            while (check && count-- > 0) {
                for (MapLocation x : objects) {
                    if (x != null && x.x != 0 && x.y != 0) {
                        for (int j = 0; j < 8; j++) {
                            if (x.distanceSquaredTo(next) < 27) {
                                Direction nDir = (right) ? dir.rotateRight() : dir
                                        .rotateLeft(); // turn right or left
                                next = myLoc.add(nDir);
                            }
                        }
                    }
                }
                // reached the end to the next location is good
                check = false;
            }

            //curve toward target
            //boolean canChange = true;
            Direction direct = myLoc.directionTo(target);
            Direction d = myLoc.directionTo(next);
            do {
                rc.setIndicatorDot(myLoc, 4, 4, 4);
                if (right) {
                    d = d.rotateRight();
                } else {
                    d = d.rotateLeft();
                }
            } while (!inTowerRange(myLoc.add(d), objects) && d.compareTo(direct) != 0);
            dir = d;
            if (rc.senseTerrainTile(next) == TerrainTile.OFF_MAP) {
                dir = dir.opposite();
            }
            if (rc.isCoreReady()) {
                for (int i = 0; i < 8; i++) {
                    if (rc.canMove(dir) && !inTowerRange(myLoc.add(dir), objects)) {
                        pre = dir;
                        rc.move(dir);
                        break;
                    } else {
                        dir = (right) ? dir.rotateRight() : dir.rotateLeft();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("kiting pooped out" + e);
        }
    }
    
    /**
     * Test in range of incoming objects (uses hard set 27 sq at the moment)
     *
     * @param m location to check
     * @param obj stuff to avoid
     * @return
     */
    public static boolean inTowerRange(MapLocation m, MapLocation[] obj, int range) {
        for (MapLocation x : obj) {
            if (x != null && x.x != 0 && x.y != 0) {
                if (m.distanceSquaredTo(x) < range) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Test in range of incoming objects (uses hard set 27 sq at the moment)
     *
     * @param m location to check
     * @param obj stuff to avoid
     * @return
     */
    public static boolean inTowerRange(MapLocation m, MapLocation[] obj) {
        for (MapLocation x : obj) {
            if (x != null && x.x != 0 && x.y != 0) {
                if (m.distanceSquaredTo(x) < 27) {
                    return true;
                }
            }
        }
        return false;
    }

    public void tryFly (MapLocation m) throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        try {
        	Direction d = rc.getLocation().directionTo(m);
            int offsetIndex = 0;
            int[] offsets = {0, 1, -1, 2, -2};
            int dirint = Constants.directionToInt(d);
            while (offsetIndex < 5
                    && !rc.canMove(Constants.directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5 && rc.isCoreReady()) {
                rc.move(Constants.directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            }
        } catch (Exception e) {
            System.out.println("Error in tryFly");
        } 
    }

}
