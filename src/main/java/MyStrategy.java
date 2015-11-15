import model.*;


import java.util.Arrays;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    //Сохранение состояния переменных
    private static double prevGetX=-1.0;
    private static double prevGetY=-1.0;

    private static boolean isStart=false;           //Гонка началась

    private static final int ticksStuckIni=10;      //Количество тиков за которые если координаты не меняются - машина застряла
    private static int ticksStuck=ticksStuckIni;   //Тиков до застревания
    private static boolean carStuck=false;          //Машина застряла
    private static int ticksGetOutStuckIni=250;      //Количество тиков за которые машина пытается выбраться
    private static int ticksGetOutStuck=ticksGetOutStuckIni;      //Тиков до продолжения движения
    private static boolean carGetOutStuckOperation=false;          //Операция по вызволению машины активирована

    private static final int ticksToComeBack=50;
    //Задание движения
    @Override
    public void move(Car self, World world, Game game, Move move) {
        if(!carStuck) {
            double nextWaypoint[] = new double [1];
            nextWaypoint = getDirection(self, world, game, move);
            moveTo(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);
//            move.setEnginePower(0.7D);
            //Если игра началась
            if (world.getTick() > game.getInitialFreezeDurationTicks() + 50) {

                if(move.getEnginePower()>0){
                    isStart=true;
                }

                if (self.getNitroChargeCount() > 0 && self.getRemainingNitroCooldownTicks() == 0 && !move.isUseNitro()) {
                    if(self.getDistanceTo(nextWaypoint[0], nextWaypoint[1])>3000){
                        move.setUseNitro(true);
                    }
                }

            }

            isStuck(self, world, game, move);

        }else{
            //Если мы застряли
            getOutOfStuck(self, world, game, move);
        }

        attack(self, world, game, move);
        //Обновляем наше местоположение
        prevGetX=self.getX();
        prevGetY=self.getY();
    }

    //Устанавливаем следующий checkPoint
    public double[] getDirection(Car self, World world, Game game, Move move){
        double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
        double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();
        double cornerTileOffset = 0.3D * game.getTrackTileSize();

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

        return new double []{nextWaypointX,nextWaypointY};
    }

    //Задаём манеру передвижения при нормальных условиях
    public void moveTo(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY){

        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleToWaypoint * 12.0D / PI);
        move.setEnginePower(0.5D);

        double coefBrake = 5.5D * 5.5D * PI;
        if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake) {
            move.setSpillOil(true);
            move.setBrake(!move.isBrake());
        }
        return;
    }

    //Проверка на застревание
    public void isStuck(Car self, World world, Game game, Move move){
        //Если гонка идёт и мы целы, а координаты не меняются уже ticksStuckIni тиков
        if(isStart & self.getDurability()>0){

            double dX = prevGetX-self.getX();
            double dY = prevGetY-self.getY();
            double delta = 1;
            if (Math.abs(dX) <= delta && Math.abs(dY) <= delta) {
                ticksStuck--;//Запускаем обратный отсчёт
                //Если количество тиков вышло - машина застряла
                if(ticksStuck<0) {
                    double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
                    double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();

                    carStuck = true;
                    ticksGetOutStuck=ticksGetOutStuckIni;
                }
            }else {
                //Если машина сдвинулась с места - обновляем счётчик
                ticksStuck=ticksStuckIni;
            }
        }
        return;
    }

    //Стратегия возврата в гонку, если машина застряла
    public void getOutOfStuck(Car self, World world, Game game, Move move){
        if(carGetOutStuckOperation){
            ticksGetOutStuck--;

            double delta = 0.1;
            if(Math.abs(self.getEnginePower())<= delta){
                move.setEnginePower(-1.0D);//Иначе машина останется стоять
                double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
                double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();
                double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
                move.setWheelTurn(-angleToWaypoint * 12.0D / PI);
            }
            if(ticksGetOutStuck<0) {
                carStuck = false;
                carGetOutStuckOperation=false;
                ticksStuck=ticksStuckIni*10;
            }
        }else{
            move.setBrake(false);
            move.setEnginePower(-1.0D);
//            move.setWheelTurn(-self.getWheelTurn());
            carGetOutStuckOperation=true;
        }
        return;
    }

//    //Координаты ближайшего оппонента
//    public double[] getClosestOponentList(Car self, World world, Game game, Move move){
//        Car cars[] = world.getCars();
//        double carClosest[]=new double[10];
//        int i=0;
//        for(Car car :cars ){
//            carClosest[i]= new double [self.getDistanceTo(car.getX(),car.getY()];
//            i++;
//        }
//        Arrays.sort(carClosest);
//        return carClosest;
//    }


    //Действия с бонусами
    public void attack(Car self, World world, Game game, Move move){
        if (self.getProjectileCount() > 0) {
            Car cars[] = world.getCars();
            double carClosest[]=new double[4];
            int i=0;
            for(Car car :cars ){
                carClosest[i]= self.getDistanceTo(car.getX(),car.getY());
                i++;
            }
            Arrays.sort(carClosest);

            //Если противник в пределах видимости, то атакуем
            if(carClosest[1]<1100){

                i=0;
                for(Car car :cars ){
                    double delta = 1;
                    if(Math.abs(carClosest[i]-self.getDistanceTo(car.getX(),car.getY()))<= delta){
                        double angleToOpponent = self.getAngleTo(car.getX(),car.getY());
                        delta = 0.3;
                        if(Math.abs(angleToOpponent)<= delta){
                            move.setThrowProjectile(true);

                        }
                    }
                    i++;
                }
            }

        }
        return;
    }
}
//getCarWheelTurnChangePerTick - максимальное значение, на которое может измениться относительный угол поворота колёс кодемобиля (❝❛r✳✇❤❡❡❧❚✉r♥) за один тик.

