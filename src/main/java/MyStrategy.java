import model.*;


import java.util.Arrays;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    //Сохранение состояния переменных
    private static double prevGetX=-1.0;
    private static double prevGetY=-1.0;

    private static boolean isStart=false;           //Гонка началась

    private static final int ticksStuckIni=15;      //Количество тиков за которые если координаты не меняются - машина застряла
    private static int ticksStuck=ticksStuckIni;   //Тиков до застревания
    private static boolean carStuck=false;          //Машина застряла
    private static int ticksGetOutStuckIni=70;      //Количество тиков за которые машина пытается выбраться
    private static int ticksGetOutStuck=ticksGetOutStuckIni;      //Тиков до продолжения движения
    private static boolean carGetOutStuckOperation=false;          //Операция по вызволению машины активирована

    private static double maxEngineValue=1.0D;          //Максимальное значение двигателя
    private static double goodWheelTurn = 0;

    private static boolean firstTick = true;
    private static int[][] grafWay;
    //Задание движения
    @Override
    public void move(Car self, World world, Game game, Move move) {
        if(firstTick)
            firstCheck(world);

        if(!carStuck) {
            double nextWaypoint[] = new double [1];
            nextWaypoint = getDirection(self, world, game, move);
            moveTo(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);
//            move.setEnginePower(0.3D);
            //Если игра началась
            if (world.getTick() > game.getInitialFreezeDurationTicks()) {

                if(self.getEnginePower()>0){
                    isStart=true;
                }

                if (self.getNitroChargeCount() > 0 && self.getRemainingNitroCooldownTicks() == 0 && !move.isUseNitro()) {
                    if(self.getDistanceTo(nextWaypoint[0], nextWaypoint[1])>2500){
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

//        double cornerTileOffset = 0.3D * game.getTrackTileSize();
        double cornerTileOffset;
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

//        System.out.println(world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()].toString());
        return new double []{nextWaypointX,nextWaypointY};
    }

    //Задаём манеру передвижения при нормальных условиях
    public void moveTo(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY){
        //Если участок прямой, то применяем стандартную стратегию
        if(nextWayToCheckpointIsStraightLine(self, world, game, move)) {
            double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
            double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

            move.setWheelTurn(angleToWaypoint * 12.0D / PI);
            move.setEnginePower(maxEngineValue);

            double coefBrake = 5.5D * 5.5D * PI;
            if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake) {
                move.setSpillOil(true);
                move.setBrake(!move.isBrake());
            }
        }else{
            //Если участок сложный, то применяем спец.стратегию
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
//        System.out.println(nextWayToCheckpointIsStraightLine(self, world, game, move));
    }

    public boolean nextWayToCheckpointIsStraightLine(Car self, World world, Game game, Move move) {
        //Определение оптимальности маршрута
        int wpX = self.getNextWaypointX();
        int wpY = self.getNextWaypointY();
        int wpIndex = self.getNextWaypointIndex();

        int selfX = (int) (self.getX() / game.getTrackTileSize());
        int selfY = (int) (self.getY() / game.getTrackTileSize());

        int deltaX = wpX - selfX;
        int deltaY = wpY - selfY;
        //Если не на прямой, то сразу применяем альтернативную тактику
        if (Math.abs(deltaX) > 0 && Math.abs(deltaY) > 0) {
            //Устанавливаем промежуточный WayPoint
            return false;
        } else {
            //Проверяем есть ли соединение на прямых тайлах

            int counterTiles = 0;
            //Едем по горизонтали
            if (deltaX != 0) {
                int i = wpX > selfX ? selfX : wpX;
                int iLim = wpX > selfX ? wpX : selfX;
                int j = selfY;


                for (; i < iLim; i++) {
                    TileType type = world.getTilesXY()[i][j];
                    switch (type) {
                        case HORIZONTAL:
                        case CROSSROADS:
                        case TOP_HEADED_T:
                        case BOTTOM_HEADED_T:
                            counterTiles++;
                            break;
                        default:
                            return false;
                    }
                }
                return true;

            } else {
                int i = selfX;
                int j = wpY > selfY ? selfY : wpY;
                int jLim = wpY > selfY ? wpY : selfY;

                for (; j < jLim; j++) {
                    TileType type = world.getTilesXY()[i][j];
                    switch (type) {
                        case VERTICAL:
                        case CROSSROADS:
                        case RIGHT_HEADED_T:
                        case LEFT_HEADED_T:
                            counterTiles++;
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }


        }
//        System.out.println("1");
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

                if(self.getEnginePower()>0){
                    //Если машина готова к езде, выходим из операции спасения
                    carStuck = false;
                    carGetOutStuckOperation=false;
                    ticksStuck=ticksStuckIni;
                }else if (self.getEnginePower()>-0.3D) {
                    setGoodWheelTurn(self, world, game, move, getDirection(self, world, game, move));
                    move.setWheelTurn(goodWheelTurn);
                }
            }
        } else{
            move.setBrake(true);
            carGetOutStuckOperation=true;
        }
    }

    //Оценка возможности атаки
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



    public static void firstCheck(World world) {
        System.out.println("TILES");
        for(int i=0; i<world.getWidth()-1; i++){
            System.out.print(i + "\t\t\t\t\t\t");
        }
        System.out.println(world.getWidth()-1);

        for(int j=0; j<world.getHeight(); j++){
            for(int i=0; i<world.getWidth()-1; i++){
                TileType type = world.getTilesXY()[i][j];
                StringBuilder text=new StringBuilder();
                if(i==0)
                    text.append(j+"|");//Добавляем номер строчки, на первом столбце

                text.append(type);
                switch (type) {
                    case EMPTY:
                        System.out.print(text + "\t\t\t\t\t");
                        break;
                    case HORIZONTAL:
                    case VERTICAL:
                    case CROSSROADS:
                        System.out.print(text + "\t\t\t\t");
                        break;
                    case LEFT_BOTTOM_CORNER:
                        System.out.print(text + "\t");
                        break;
                    case RIGHT_BOTTOM_CORNER:
                        System.out.print(text + "\t\t");
                        break;
                    case LEFT_TOP_CORNER:
                        if(i==0)
                            System.out.print(text + "\t\t");
                        else
                            System.out.print(text + "\t\t\t");
                        break;
                    default:
                        System.out.print(text + "\t\t\t");
                        break;

                }
            }
            System.out.println(world.getTilesXY()[world.getWidth()-1][j]);
        }

        System.out.println("WAYPOINTS");
        for(int i=0; i<world.getWaypoints().length; i++){
            //[0] - X, [1] - Y
            System.out.print("X: " + world.getWaypoints()[i][0]);
            System.out.print (" Y: " + world.getWaypoints()[i][1] + "  |  ");
        }
        firstTick=false;


//        //Построение неориентированного графа grafWay
//        grafWay = new int[world.getWidth()][world.getHeight()];
//
//        for(int j=0; j<world.getHeight(); j++){
//            for(int i=0; i<world.getWidth(); i++){
//                TileType type = world.getTilesXY()[i][j];
//                grafWay[i][j]=1;
//                switch (type) {
//                    case EMPTY:
//                        System.out.print(text + "\t\t\t\t\t");
//                        break;
//                    case HORIZONTAL:
//                    case VERTICAL:
//                    case CROSSROADS:
//                        System.out.print(text + "\t\t\t\t");
//                        break;
//                    case LEFT_BOTTOM_CORNER:
//                        System.out.print(text + "\t");
//                        break;
//                    case RIGHT_BOTTOM_CORNER:
//                        System.out.print(text + "\t\t");
//                        break;
//                    case LEFT_TOP_CORNER:
//                        if(i==0)
//                            System.out.print(text + "\t\t");
//                        else
//                            System.out.print(text + "\t\t\t");
//                        break;
//                    default:
//                        System.out.print(text + "\t\t\t");
//                        break;
//
//                }
    }
}
//getCarWheelTurnChangePerTick - максимальное значение, на которое может измениться относительный угол поворота колёс кодемобиля (❝❛r✳✇❤❡❡❧❚✉r♥) за один тик.

            //Оценка возможности подбора бонусов
//    public void checkBonuses(Car self, World world, Game game, Move move){
//        Bonus bonuses[] = world.getBonuses();
//        double bonusClosest[]=new double[10];
//        int i=0;
//
//        BonusType neededBonus = checkNeedBonuses(self, world, game, move);
//        boolean needBonusFound = false;
//        Bonus getBonus;
//        for(Bonus bonus :bonuses ){
//            if (self.getDistanceTo(bonus.getX(),bonus.getY())<500){
//                if (bonus.getType()==neededBonus){
//                    if(needBonusFound){
//                        getBonus=self.getDistanceTo(getBonus.getX(),getBonus.getY())<self.getDistanceTo(bonus.getX(),bonus.getY())
//                    }
//                    =
//                }
//            }
//
//            bonusClosest[i]= self.getDistanceTo(bonus.getX(),bonus.getY());
//            i++;
//        }
//        Arrays.sort(bonusClosest);
//
//        //Если противник в пределах видимости, то атакуем
//        if(bonusClosest[1]<1100) {
//
//            i = 0;
//            for (Bonus bonus : bonuses) {
//                double delta = 1;
//                if (Math.abs(bonusClosest[i] - self.getDistanceTo(car.getX(), car.getY())) <= delta) {
//                    double angleToOpponent = self.getAngleTo(car.getX(), car.getY());
//                    delta = 0.15;
//                    if (Math.abs(angleToOpponent) <= delta) {
//                        move.setThrowProjectile(true);
//
//                    }
//                }
//                i++;
//
//            }
//        }
//
//    //Оценка нужды в бонусах
//    public BonusType checkNeedBonuses(Car self, World world, Game game, Move move){
//            if(self.getDurability()<0.4){
//                return BonusType.REPAIR_KIT;
//            }else{
//                double repairNeed = (self.getDurability()-1);
//                double ammoNeed= 1/(self.getProjectileCount()+0.2);
//                double nitroNeed= 1/(self.getNitroChargeCount()+0.05);
//                double oilNeed= 1/(self.getOilCanisterCount()+0.1);
//
//                if(ammoNeed>nitroNeed){
//                    if(ammoNeed>oilNeed){
//                        if(ammoNeed>oilNeed){
//
//                        }else{
//
//                        }
//                }
////                        Player playersScore[] = new Player [4];
////                for(Player player :world.getPlayers() ){
////                    bonusClosest[i]= self.getDistanceTo(bonus.getX(),bonus.getY());
////                    i++;
////                }
////                Arrays.sort(bonusClosest);getScore( )
//        }
//        }
