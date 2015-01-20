package team163000;

import team163000.units.Beaver;
import team163000.units.Drone;
import team163000.units.HQ;
import team163000.units.Helipad;
import team163000.units.Miner;
import team163000.units.MinerFactory;
import team163000.units.SupplyDepot;
import team163000.units.Tower;
import team163000.units.Unit;
import battlecode.common.RobotController;
import battlecode.common.RobotType;


public class RobotPlayer {

	public static void run(RobotController tomatojuice) {
		Unit u = make(tomatojuice.getType());
		if (u != null) {
			u.onSpawn(tomatojuice);
			u.run();
		} else {
			System.out.println(tomatojuice.getType());
		}
		
	}
	
	public static Unit make(RobotType rt) {
		switch (rt) {
		case BEAVER : return new Beaver();
		case DRONE : return new Drone();
		case MINER : return new Miner();
		case HELIPAD : return new Helipad();
		case MINERFACTORY : return new MinerFactory();
		case SUPPLYDEPOT : return new SupplyDepot();
		case HQ : return new HQ();
		case TOWER : return new Tower();
		default : {
			System.out.println(rt);
			return null;
		}
		}
	}
}
