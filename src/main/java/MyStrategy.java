//import model.Car;
//import model.Game;
//import model.Move;
//import model.World;
//
//public final class MyStrategy implements Strategy {
//    @Override
//    public void move(Car self, World world, Game game, Move move) {
//        world.getWaypoints();
//
//        move.setEnginePower(1.0D);
//        move.setThrowProjectile(true);
//        move.setSpillOil(true);
//
//        self.getWheelTurn();
//        move.setWheelTurn();
//        move.setBrake();
//
//        if (world.getTick() > game.getInitialFreezeDurationTicks()) {
//            move.setUseNitro(true);
//        }
//    }
//}

import model.Car;
import model.Game;
import model.Move;
import model.World;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    private static double prevGetX=-1.0;
    private static double prevGetY=-1.0;
    private static boolean isStart=true;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
        double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();
        double cornerTileOffset = 0.5D * game.getTrackTileSize();

//        switch (world.getTilesXY()[(int)self.getX()][(int)self.getY()]) {
//            case VERTICAL:
//            case HORIZONTAL:
//                move.setUseNitro(true);
//                break;
//            default:
//        }
        switch (world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()]) {
            case LEFT_TOP_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case RIGHT_TOP_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            default:
        }

        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleToWaypoint * 12.0D / PI);
        move.setEnginePower(1D);

        double coefBrake = 5.5D * 5.5D * PI;
        if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake){
            move.setSpillOil(true);
            move.setBrake(!move.isBrake());
        }

        prevGetX=self.getX();
        prevGetY=self.getY();

    }
}