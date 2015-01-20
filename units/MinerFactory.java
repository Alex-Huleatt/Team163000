package team163000.units;

import java.util.Random;

import team163.utils.CHANNELS;
import team163000.Constants;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class MinerFactory implements Unit {

	RobotController rc;
	Random rand;
	Team myTeam;
	public void onSpawn(RobotController rc) {
		this.rc = rc;
		rand = new Random();
		myTeam = rc.getTeam();
	}

	public void run() {
		while (true) {
			try {
				// get information broadcasted by the HQ
				int numMiners = rc.readBroadcast(CHANNELS.NUMBER_MINER.getValue());
				int maxMiners = rc.readBroadcast(CHANNELS.BUILD_NUM_MINER.getValue());
				if (rc.isCoreReady() && rc.getTeamOre() >= 250 && numMiners < maxMiners) {
					Constants.trySpawn(Constants.directions[rand.nextInt(8)], RobotType.MINER, rc);
				}
				rc.yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
}
