package team163000.behavior;

import team163000.units.Unit;
import battlecode.common.*;

public interface Behavior {
	
	/* general concepts of an agent */
	void perception();
	void calculation();
	void action();
	
	/* in case of a global panic response */
	void panicAlert();
	
	void giveUnit(Unit u);

}
