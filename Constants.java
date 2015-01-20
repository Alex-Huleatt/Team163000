package team163000;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Constants {
    public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST,
        Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH,
        Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    public static void tryMove(RobotController rc, Direction d) {
        try {
            int offsetIndex = 0;
            int[] offsets = {0, 1, -1, 2, -2};
            int dirint = directionToInt(d);
            while (offsetIndex < 5
                    && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
                offsetIndex++;
            }
            if (offsetIndex < 5 && rc.isCoreReady()) {
                rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            }
        } catch (Exception e) {
            System.out.println("Error in tryMove");
        }
    }
    
    public static void trySpawn(Direction d, RobotType type, RobotController rc) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        while (offsetIndex < 8
                && !rc.canSpawn(
                        directions[(dirint + offsets[offsetIndex] + 8) % 8],
                        type)) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
        }
    }
    public static int directionToInt(Direction d) {
        switch (d) {
            case NORTH:
                return 0;
            case NORTH_EAST:
                return 1;
            case EAST:
                return 2;
            case SOUTH_EAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTH_WEST:
                return 5;
            case WEST:
                return 6;
            case NORTH_WEST:
                return 7;
            default:
                return -1;
        }
    }
}
