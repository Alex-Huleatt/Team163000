package team163000.units;

import team163000.AttackUtils;
import battlecode.common.*;

/**
 * Created by brentechols on 1/5/15.
 * sweetness
 */
public class Tower implements Unit {

    double myHealth;
    MapLocation myLoc;
    RobotController rc;
    
    public void onSpawn(RobotController rc) {
    	myHealth = rc.getHealth();
        myLoc = rc.getLocation();
        this.rc = rc;
    }

    public void run() {
        
        while (true) {
            try {
                double curHealth = rc.getHealth();
                if (curHealth < myHealth) {
                    myHealth = curHealth;
                    rc.broadcast(911, myLoc.x);
                    rc.broadcast(912, myLoc.y);
                    continue;
                }
                if (rc.isWeaponReady()) {
                    AttackUtils.attackSomething(rc, rc.getType().attackRadiusSquared, rc.getTeam().opponent());
                }
            } catch (Exception e) {
                System.out.println("Tower Exception");
                e.printStackTrace();
            }
        }
    }
}
