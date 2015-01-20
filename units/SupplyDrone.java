/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team163000.units;

import battlecode.common.*;

import java.util.Random;

import javax.xml.stream.Location;

import team163000.CHANNELS;
import team163000.Constants;

/**
 *
 * @author sweetness
 */
public class SupplyDrone implements Unit {

    public Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST,
        Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public RobotController rc;
    public Team myTeam;
    public Team enemyTeam;
    public int myRange;
    public Random rand;
    public MapLocation myLoc;
    public int lastHead = 0;
    public MapLocation travelLoc;
    public boolean wasReturning = false;

    public int myHead = -1;
    public int turnsWaited = 0;
    
    private int offset;
    
    public SupplyDrone(int offset) {
    	this.offset = offset;
    }
    
    public void onSpawn(RobotController rc) {
    	this.rc = rc;
        rand = new Random(rc.getID());
        myRange = rc.getType().attackRadiusSquared;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
    }

    public void run() {
        while (true) {
            try {
                //Broadcast to the world that there is a supply drone!
                rc.broadcast(CHANNELS.SUPPLY_DRONE1.getValue() - 1 + offset, Clock.getRoundNum());

                myLoc = rc.getLocation();
                rc.setIndicatorString(0, "I am supply beaver | head: " + myHead + " tail: " + rc.readBroadcast(197));
                rc.setIndicatorString(2, "Actual head: " + rc.readBroadcast(196) + " tail: " + rc.readBroadcast(197));

                //broadcast our updated position
                rc.broadcast(198, myLoc.x);
                rc.broadcast(199, myLoc.y);

                if (rc.isCoreReady()) {
                    double supply = rc.getSupplyLevel();
                    if (supply > 500) {
                        //System.out.println("Supplying people!");
                        goSupplyPeople();
                    } else {
                        //System.out.println("Going back to base!");
                        goToBase();
                    }
                }

                rc.yield();
            } catch (Exception e) {
                System.out.println("Supply Beaver Exception");
                e.printStackTrace();
            }
        }
    }

    // THIS IS CALLED BY OTHER PEOPLE SO DONT USE CLASS VARIABLES
    public static int requestResupply(RobotController requester, MapLocation loc, int channel) throws GameActionException {
        // Default assign the channel to the tail
        if (channel == 0) {
            channel = requester.readBroadcast(197);

            // Adjust the tail pointer for new requests
            channel = (channel == 300) ? 200 : channel;
            requester.broadcast(197, channel + 2);
        }

        // Write out the supply request to the radio
        requester.broadcast(channel, loc.x);
        requester.broadcast(channel + 1, loc.y);

        return channel;
    }

    void goToBase() throws GameActionException {
        rc.setIndicatorString(1, "Heading back to base!");
        wasReturning = true;
        if (travelLoc != null && !travelLoc.equals(rc.senseHQLocation())) {
            travelLoc = null;
        }

        MapLocation hqLoc = rc.senseHQLocation();
        if (myLoc.distanceSquaredTo(hqLoc) <= GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED) {
            rc.setIndicatorString(1, "Resupplying!");
            return;
        }

        usePathMove(hqLoc);
    }

    void goSupplyPeople() throws GameActionException {
        if (wasReturning) {
            travelLoc = null;
        }
        wasReturning = false;

        int head = rc.readBroadcast(196);
        int tail = rc.readBroadcast(197);

        // If there is no one to supply, go collect more supplies
        if (head == tail && myHead == -1) {
            //System.out.println("Queue is empty, no work to do!");
            goToBase();
            return;
        }

        if (myHead == -1) {
            claimDelivery();
        }

        MapLocation dest = new MapLocation(rc.readBroadcast(myHead), rc.readBroadcast(myHead + 1));
        rc.setIndicatorString(1, "Supplying People! " + dest.toString());
        if (myLoc.distanceSquaredTo(dest) < GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED && Clock.getBytecodeNum() < 4000) {
            RobotInfo info = rc.senseRobotAtLocation(dest);
            if (info == null) {
                turnsWaited++;
            } else {
                rc.transferSupplies((int) Math.min(rc.getSupplyLevel() - 500, 2000), dest);
                completeDelivery();
            }

            if (turnsWaited == 3) {
                completeDelivery();
            }
        }

        usePathMove(dest);
    }

    void claimDelivery() throws GameActionException {
        myHead = rc.readBroadcast(196);
        rc.broadcast(196, (myHead == 298) ? 200 : (myHead + 2));
    }

    void completeDelivery() {
        myHead = -1;
        turnsWaited = 0;
    }

    void usePathMove(MapLocation dest) throws GameActionException {
        tryKite(dest, rc.senseEnemyTowerLocations());
    }
    
    /**
     * Does not take into account walls and trys to kite around towers and stuff
     *
     * @param target map location to go to
     * @param objects stuff to kite around such as tower locations
     */
    public Direction pre = Direction.NORTH;
    public boolean right = true;
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
    public boolean inTowerRange(MapLocation m, MapLocation[] obj, int range) {
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
    public boolean inTowerRange(MapLocation m, MapLocation[] obj) {
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
