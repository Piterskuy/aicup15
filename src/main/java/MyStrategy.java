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
    private static int ticksGetOutStuckIni=80;      //Количество тиков за которые машина пытается выбраться
    private static int ticksGetOutStuck=ticksGetOutStuckIni;      //Тиков до продолжения движения
    private static boolean carGetOutStuckOperation=false;          //Операция по вызволению машины активирована
    private static boolean carGetOutStuckOperationForward=false;          //Операция по вызволению машины2 активирована

    private static double maxEngineValue=1.0D;          //Максимальное значение двигателя


    private static double goodWheelTurn = 0;

    private static final int ticksToComeBack=50;
    //Задание движения
    @Override
    public void move(Car self, World world, Game game, Move move) {
        if(!carStuck) {
            double nextWaypoint[] = new double [1];
            nextWaypoint = getDirection(self, world, game, move);
            moveTo(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);
//            move.setEnginePower(0.3D);
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

            isStuck(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);

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
    public double[] getDirection(Car self, World world, Game game, Move move) {
        double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
        double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();

        double cornerTileOffset = 0.3D * game.getTrackTileSize();

        if (move.isUseNitro()) {
            cornerTileOffset = 0.6D * game.getTrackTileSize();
        } else {
            cornerTileOffset = 0.3D * game.getTrackTileSize();
        }

        switch (world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()]) {
            case LEFT_TOP_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                maxEngineValue = 0.9D;
                break;
            case RIGHT_TOP_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                maxEngineValue = 0.9D;
                break;
            case LEFT_BOTTOM_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                maxEngineValue = 0.9D;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                maxEngineValue = 0.9D;
                break;
            case HORIZONTAL:
            case VERTICAL:
            case CROSSROADS:
                maxEngineValue = 1.0D;
                break;
            default:
                maxEngineValue = 0.7D;
        }
        if (self.getRemainingNitroCooldownTicks() > 0)
            maxEngineValue /= 1.3D;

        System.out.println(world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()].toString());
        return new double []{nextWaypointX,nextWaypointY};
    }

    //Задаём манеру передвижения при нормальных условиях
    public void moveTo(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY){

        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleToWaypoint * 12.0D / PI);
        move.setEnginePower(maxEngineValue);

        double coefBrake = 5.5D * 5.5D * PI;
        if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake) {
            move.setSpillOil(true);
            move.setBrake(!move.isBrake());
        }
    }

    //Проверка на застревание
    public void isStuck(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY){
        //Если гонка идёт и мы целы, а координаты не меняются уже ticksStuckIni тиков
        if(isStart & self.getDurability()>0){

            double dX = prevGetX-self.getX();
            double dY = prevGetY-self.getY();
            double delta = 0.5;
            if (Math.abs(dX) + Math.abs(dY) <= delta) {
                ticksStuck--;//Запускаем обратный отсчёт
                //Если количество тиков вышло - машина застряла
                if(ticksStuck<0) {
                    carStuck = true;
                    ticksGetOutStuck=ticksGetOutStuckIni;
//                    moveTo(self, world, game, move, nextWaypointX, nextWaypointY);
                    goodWheelTurn = move.getWheelTurn();
                }
            }else {
                //Если машина сдвинулась с места - обновляем счётчик
                ticksStuck=ticksStuckIni;
            }
        }
    }


    public void setGoodWheelTurn(Car self, World world, Game game, Move move,double nextWaypoint[]){
        double angleToWaypoint = self.getAngleTo(nextWaypoint[0], nextWaypoint[1]);
        goodWheelTurn =angleToWaypoint * 12.0D / PI;
    }

    //Стратегия возврата в гонку, если машина застряла
    public void getOutOfStuck(Car self, World world, Game game, Move move){
        //Когда начинаем выбираться, едем назад
        if(carGetOutStuckOperation){
            ticksGetOutStuck--;
            move.setEnginePower(-1.0D);//Едем назад
            move.setWheelTurn(-goodWheelTurn);

            if(ticksGetOutStuck<=0) {
                //Уходим по таймеру в минус и ждём пока машина затормозит после заднего хода
                move.setEnginePower(1.0D);//Едем вперёд
                setGoodWheelTurn(self, world, game, move, getDirection(self, world, game, move));
                move.setWheelTurn(goodWheelTurn);

                if(self.getEnginePower()>0){
                    //Если машина готова к езде, выходим из операции спасения
                    carStuck = false;
                    carGetOutStuckOperation=false;
                    ticksStuck=ticksStuckIni;
                }
            }
        } else{
            move.setBrake(true);
            carGetOutStuckOperation=true;
        }
    }

    //Действия с бонусами
    public void attack(Car self, World world, Game game, Move move){
        if (isStart & self.getProjectileCount() > 0 & self.getRemainingProjectileCooldownTicks()<=0) {
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
                        delta = 0.15;
                        if(Math.abs(angleToOpponent)<= delta){
                            move.setThrowProjectile(true);

                        }
                    }
                    i++;
                }
            }

        }
    }
}
//getCarWheelTurnChangePerTick - максимальное значение, на которое может измениться относительный угол поворота колёс кодемобиля (❝❛r✳✇❤❡❡❧❚✉r♥) за один тик.

