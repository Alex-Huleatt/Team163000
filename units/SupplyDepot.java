package team163000.units;

import java.util.Random;

import battlecode.common.Direction;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class SupplyDepot implements Unit {
	RobotController rc;

	@Override
	public void onSpawn(RobotController rc) {
		this.rc = rc;
	}

	@Override
	public void run() {
		try {
			while (true) {
				rc.yield();
			}
		} catch (Exception e) {
			System.out.println("Supply Depot Exception");
			e.printStackTrace();
		}
	}


}
