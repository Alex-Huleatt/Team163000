package team163000.units;

import java.util.Random;

import team163000.behavior.B_Scout;
import team163000.behavior.Behavior;
import battlecode.common.*;

public class ScoutDrone implements Unit {

    private RobotController rc;
    private int range;
    private Team team;
    private Team opponent;
    private MapLocation hq;
    private MapLocation enemyHQ;
    private boolean right; //turn right
    private boolean panic = false;
    private Behavior mood;
    
    public void onSpawn(RobotController rc) {
        this.rc = rc;
        this.range = rc.getType().attackRadiusSquared;
        this.team = rc.getTeam();
        this.opponent = rc.getTeam().opponent();
        this.hq = rc.senseHQLocation();
        this.enemyHQ = rc.senseEnemyHQLocation();

        mood = new B_Scout(); /* starting behavior of turtling */
    }

    public void run() {
        try {

            while (true) {

                /* get behavior */
                //mood = chooseB();
                MapLocation myLoc = rc.getLocation();
                double best = rc.readBroadcast(1000);
                double here = rc.senseOre(myLoc);
                MapLocation bestLoc = new MapLocation(rc.readBroadcast(1001), rc.readBroadcast(1002));
                if (here > best || (here == best && myLoc.distanceSquaredTo(hq) < bestLoc.distanceSquaredTo(hq))) {
                    rc.broadcast(1000, (int) (.5 + here));
                    rc.broadcast(1001, myLoc.x);
                    rc.broadcast(1002, myLoc.y);
                }
                /* perform round */
                mood.perception();
                mood.calculation();
                mood.action();

                /* end round */
                rc.yield();
            }
        } catch (Exception e) {
            System.out.println("Drone Exception " + e);
        }
    }
}
