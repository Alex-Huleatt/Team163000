package team163000.units;

import team163000.CHANNELS;
import team163000.Spawn;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Helipad implements Unit {
	RobotController rc;

	public void onSpawn(RobotController rc) {
		this.rc = rc;

	}

	public void run() {
		try {

			while (true) {
				int maxDrone = rc.readBroadcast(CHANNELS.BUILD_NUM_DRONE.getValue());
				int droneCount = rc.readBroadcast(CHANNELS.NUMBER_DRONE.getValue());
				if (rc.isCoreReady() && rc.getTeamOre() >= 125
						&& droneCount < maxDrone) {
					Spawn.randSpawn(
							RobotType.DRONE, rc);
				}

				rc.yield();
			}
		} catch (Exception e) {
			System.out.println("Helipad Exception");
			e.printStackTrace();
		}
	}

}
