import model.*;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    //Сохранение состояния переменных
    private static double prevGetX = -1.0;
    private static double prevGetY = -1.0;

    private static boolean isStart = false;           //Гонка началась

    private static final int ticksStuckIni = 15;      //Количество тиков за которые если координаты не меняются - машина застряла
    private static int ticksStuck = ticksStuckIni;   //Тиков до застревания
    private static boolean carStuck = false;          //Машина застряла
    private static int ticksGetOutStuckIni = 70;      //Количество тиков за которые машина пытается выбраться
    private static int ticksGetOutStuck = ticksGetOutStuckIni;      //Тиков до продолжения движения
    private static boolean carGetOutStuckOperation = false;          //Операция по вызволению машины активирована

    private static double maxEngineValue = 1.0D;          //Максимальное значение двигателя
    private static double goodWheelTurn = 0;

    private static boolean firstTick = true;
//    private static int[][] myWayPoints;
    private static Map<ExampleNode> myMap;
    private static List<ExampleNode> path;
    private static int myNewTacticCount=0;
    private static boolean definedMap = true;
    private static int nitroDistance;
    private static int counterStraightTilesToNextWP;    //Упрощённый подсчёт прямых тайлов (до следующего WP)
    private static int straightTilesCounter;            //Полнный подсчёт прямых тайлов (до следующего поворота)
    private static boolean oneTileBeforeTurn;
    //Задание движения
    @Override
    public void move(Car self, World world, Game game, Move move) {
        oneTileBeforeTurn=false;
//        if(self.isFinishedTrack())
//            System.out.println("Использовано раз" + myNewTacticCount);

        if (firstTick) {
//            firstCheck(world);
            calcParams(world, game);
            makeMyWay(self, world, game, move);
//            myWayToCheckpoint(self, world, game, move);
        }
        if (!carStuck) {
            double nextWaypoint[];
            distanceToTurn(self, world, game, move);
            nextWaypoint = getDirection(self, world, game, move);
            moveTo(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);
//            move.setEnginePower(0.3D);

            //Определение начала игры
            if (world.getTick() > game.getInitialFreezeDurationTicks()) {
                if (self.getEnginePower() > 0) {
                    isStart = true;
                }
            }
            useNitro(self, world, game, move, nextWaypoint);
            isStuck(self, world, game, move, nextWaypoint[0], nextWaypoint[1]);

        } else {
            //Если мы застряли
            getOutOfStuck(self, world, game, move);
        }

        attack(self, world, game, move);
        //Обновляем наше местоположение
        prevGetX = self.getX();
        prevGetY = self.getY();
    }

    //Устанавливаем следующий checkPoint
    public double[] getDirection(Car self, World world, Game game, Move move) {
        int nextX;
        int nextY;
//        int nextWPX=world.getWaypoints()[self.getNextWaypointIndex()][0];
//        int nextWPY=world.getWaypoints()[self.getNextWaypointIndex()][1];
//
//        TileType nextWPTile = world.getTilesXY()[nextWPX][nextWPY];
//ДОПИСАТЬ


        if(definedMap & (!isWayToNextCheckpointStraight(self, world, game, move) | straightTilesCounter < 2)){
            //Стратегия Astar
            int prevWpX = (int) (self.getX() / game.getTrackTileSize());
            int prevWpY = (int) (self.getY() / game.getTrackTileSize());
            int wpX,wpY;
            if(straightTilesCounter ==1){
                int nextWP=self.getNextWaypointIndex()+1>world.getWaypoints().length-1?0:self.getNextWaypointIndex()+1;
                wpX = world.getWaypoints()[nextWP][0];
                wpY = world.getWaypoints()[nextWP][1];
                oneTileBeforeTurn=true;
            }else {
                wpX = self.getNextWaypointX();
                wpY = self.getNextWaypointY();
            }
                path = myMap.findPath(prevWpX, prevWpY, wpX, wpY);

//            for (int i = 0; i < path.size(); i++) {
//                System.out.print("(" + path.get(i).getxPosition() + ", " + path.get(i).getyPosition() + ") -> ");
//            }

                nextX = path.get(0).getxPosition();
                nextY = path.get(0).getyPosition();
                myNewTacticCount++;

        }else {
            //Простая стратегия
            nextX = self.getNextWaypointX();
            nextY = self.getNextWaypointY();
        }

        //        double nextWaypointX = (self.getNextWaypointX() + 0.45D) * game.getTrackTileSize();
        //        double nextWaypointY = (self.getNextWaypointY() + 0.45D) * game.getTrackTileSize();
        double nextWaypointX;
        double nextWaypointY;
        double speedModule = Math.abs(hypot(self.getSpeedX(), self.getSpeedY()));
//        double cornerTileOffset = 0.3D * game.getTrackTileSize();
        double cornerTileOffset;

        if (self.getRemainingNitroTicks() > 0 | speedModule>25) {
            cornerTileOffset = 0.6D * game.getTrackTileSize();//Чем меньше, тем дальше точка входа//0.4
            nextWaypointX = ( nextX + 0.45D) * game.getTrackTileSize();//0.5
            nextWaypointY = ( nextY + 0.45D) * game.getTrackTileSize();
        } else {
            cornerTileOffset = 0.3D * game.getTrackTileSize();
            nextWaypointX = ( nextX + 0.45D) * game.getTrackTileSize();//0.52
            nextWaypointY = ( nextY + 0.45D) * game.getTrackTileSize();
        }

//        if(speedModule>25){
//            cornerTileOffset /=1.2;
//            nextWaypointX = ( nextX + 0.3D) * game.getTrackTileSize();//0.52
//            nextWaypointY = ( nextY + 0.3D) * game.getTrackTileSize();
//        }
//        int coefSpeed=20;
//        cornerTileOffset = (0.3D + speedModule/coefSpeed)*game.getTrackTileSize() ;
//        nextWaypointX = ( nextX + 0.35D) * game.getTrackTileSize() + (1-1/speedModule)*game.getTrackTileSize();
//        nextWaypointY = ( nextY + 0.35D) * game.getTrackTileSize() + (1-1/speedModule)*game.getTrackTileSize();

        switch (world.getTilesXY()[nextX][nextY]) {
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

        //Перед поворотом сбавляем скорость
//        if(straightTilesCounter<=1) {
//            maxEngineValue /= 1.5;
//            if(speedModule>22)
//                move.setBrake(true);
//        }
        //Если мы в повороте
//        if(distanceToTurn(self, world, game, move)<=1){
//            maxEngineValue = 0.5D;
//        }
        if (self.getRemainingNitroCooldownTicks() > 0)
            maxEngineValue /= 1.3D;

//        System.out.println(world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()].toString());
        return new double[]{nextWaypointX, nextWaypointY};
    }

    //Задаём манеру передвижения при нормальных условиях
    public void moveTo(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY) {

        //Если участок прямой, то применяем стандартную стратегию
        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        if(oneTileBeforeTurn) {
            double coef=0.003*speedModule*speedModule+1;
            move.setWheelTurn(angleToWaypoint * coef / PI);
        }else{
            double coef=0.045*speedModule*speedModule+1;//12.0D
            move.setWheelTurn(angleToWaypoint * coef / PI);
        }
        move.setEnginePower(maxEngineValue);

        double coefBrake = 5.5D * 5.5D * PI;
        if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake) {
            move.setSpillOil(true);
            move.setBrake(!move.isBrake());
            move.setEnginePower(0.6);
            System.out.println("");
        }else if(move.isUseNitro() && self.getDistanceTo(self.getNextWaypointX(),self.getNextWaypointY())<1000){
            move.setBrake(true);
        }
//        }else if(speedModule * speedModule * abs(angleToWaypoint)<coefBrake*2.5){
//            move.setBrake(false);
//            move.setEnginePower(1.0D);
//        }

//        System.out.println(nextWayToCheckpointIsStraightLine(self, world, game, move));
    }

//    //Задаём манеру передвижения при нормальных условиях
//    public void checkEnemies(Car self, World world, Game game, Move move) {
//
//        //Если участок прямой, то применяем стандартную стратегию
//        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
//        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
//
//        move.setWheelTurn(angleToWaypoint * 12.0D / PI);
//        move.setEnginePower(maxEngineValue);
//
//        double coefBrake = 5.5D * 5.5D * PI;
//        if (speedModule * speedModule * abs(angleToWaypoint) > coefBrake) {
//            move.setSpillOil(true);
//            move.setBrake(!move.isBrake());
//            move.setEnginePower(0.6);
//            System.out.println("");
//        }else if(move.isUseNitro() && self.getDistanceTo(self.getNextWaypointX(),self.getNextWaypointY())<1000){
//            move.setBrake(true);
//        }
////        }else if(speedModule * speedModule * abs(angleToWaypoint)<coefBrake*2.5){
////            move.setBrake(false);
////            move.setEnginePower(1.0D);
////        }
//
////        System.out.println(nextWayToCheckpointIsStraightLine(self, world, game, move));
//    }

    //Проверка на застревание
    public void isStuck(Car self, World world, Game game, Move move, double nextWaypointX, double nextWaypointY) {
        //Если гонка идёт и мы целы, а координаты не меняются уже ticksStuckIni тиков
        if (isStart & self.getDurability() > 0) {

            double dX = prevGetX - self.getX();
            double dY = prevGetY - self.getY();
            double delta = 0.5;
            if (Math.abs(dX) + Math.abs(dY) <= delta) {
                ticksStuck--;//Запускаем обратный отсчёт
                //Если количество тиков вышло - машина застряла
                if (ticksStuck < 0) {
                    carStuck = true;
                    ticksGetOutStuck = ticksGetOutStuckIni;
//                    moveTo(self, world, game, move, nextWaypointX, nextWaypointY);
                    goodWheelTurn = move.getWheelTurn();
                }
            } else {
                //Если машина сдвинулась с места - обновляем счётчик
                ticksStuck = ticksStuckIni;
            }
        }
    }


    public void setGoodWheelTurn(Car self, World world, Game game, Move move, double nextWaypoint[]) {
        double angleToWaypoint = self.getAngleTo(nextWaypoint[0], nextWaypoint[1]);
        goodWheelTurn = angleToWaypoint * 12.0D / PI;
    }

    //Стратегия возврата в гонку, если машина застряла
    public void getOutOfStuck(Car self, World world, Game game, Move move) {
        //Когда начинаем выбираться, едем назад
        if (carGetOutStuckOperation) {
            ticksGetOutStuck--;
            move.setEnginePower(-1.0D);//Едем назад
            move.setWheelTurn(-goodWheelTurn);

            if (ticksGetOutStuck <= 0) {
                //Уходим по таймеру в минус и ждём пока машина затормозит после заднего хода
                move.setEnginePower(1.0D);//Едем вперёд

                if (self.getEnginePower() > 0) {
                    //Если машина готова к езде, выходим из операции спасения
                    carStuck = false;
                    carGetOutStuckOperation = false;
                    ticksStuck = ticksStuckIni;
                } else if (self.getEnginePower() > -0.3D) {
                    setGoodWheelTurn(self, world, game, move, getDirection(self, world, game, move));
                    move.setWheelTurn(goodWheelTurn);
                }
            }
        } else {
            move.setBrake(true);
            carGetOutStuckOperation = true;
        }
    }

    //Оценка возможности атаки
    public void attack(Car self, World world, Game game, Move move) {
        if (isStart & self.getProjectileCount() > 0 & self.getRemainingProjectileCooldownTicks() <= 0) {
            Car cars[] = world.getCars();
            double carClosest[] = new double[4];
            int i = 0;
            for (Car car : cars) {
                carClosest[i] = self.getDistanceTo(car.getX(), car.getY());
                i++;
            }
            Arrays.sort(carClosest);

            //Если противник в пределах видимости, то атакуем
            if (carClosest[1] < 1200) {

                i = 0;
                for (Car car : cars) {
                    if(!car.isTeammate() & !car.isFinishedTrack()) {
                        double delta = 1;
                        if (Math.abs(carClosest[i] - self.getDistanceTo(car.getX(), car.getY())) <= delta) {
                            double angleToOpponent = self.getAngleTo(car);
                            delta = 0.15;
                            if (Math.abs(angleToOpponent) <= delta) {
                                move.setThrowProjectile(true);

                            }
                        }
                        i++;
                    }
                }
            }

        }
    }

    //Оценка возможности атаки
    public void useNitro(Car self, World world, Game game, Move move, double nextWaypoint[]) {
        if (isStart & self.getNitroChargeCount() > 0 & self.getRemainingNitroCooldownTicks() <= 0 && !move.isUseNitro()) {

                if (straightTilesCounter>4 & world.getTick()>500 & self.getDurability()>0.5) {
//            double distToTurn = distanceToTurn(self, world, game, move)*game.getTrackTileSize();
//            if (distToTurn > nitroDistance*10) {

                    move.setUseNitro(true);
                }

//            if(self.getDistanceTo(nextWaypoint[0], nextWaypoint[1]) <850) {
//            }
//            Car cars[] = world.getCars();
//            double carClosest[] = new double[4];
//            int i = 0;
//            for (Car car : cars) {
//                carClosest[i] = self.getDistanceTo(car.getX(), car.getY());
//                i++;
//            }
//            Arrays.sort(carClosest);
//
//            //Если противник в пределах видимости, то атакуем
//            if (carClosest[1] < 1100) {
//
//                i = 0;
//                for (Car car : cars) {
//                    if(!car.isTeammate() & !car.isFinishedTrack()) {
//                        double delta = 1;
//                        if (Math.abs(carClosest[i] - self.getDistanceTo(car.getX(), car.getY())) <= delta) {
//                            double angleToOpponent = self.getAngleTo(car);
//                            delta = 0.15;
//                            if (Math.abs(angleToOpponent) <= delta) {
//                                move.setThrowProjectile(true);
//
//                            }
//                        }
//                        i++;
//                    }
//                }
//            }

        }
    }
    public static void calcParams(World world, Game game){
        nitroDistance=(int)game.getNitroEnginePowerFactor()*game.getNitroDurationTicks();

        System.out.println("WAYPOINTS");
        for (int i = 0; i < world.getWaypoints().length; i++) {
            //[0] - X, [1] - Y
            System.out.print("X: " + world.getWaypoints()[i][0]);
            System.out.print(" Y: " + world.getWaypoints()[i][1] + "  |  ");
        }
        firstTick = false;


    }

    //Возвращает расстояние до ближайшего поворота

    public void distanceToTurn(Car self, World world, Game game, Move move) {

        int curX = (int) (self.getX() / game.getTrackTileSize());
        int curY = (int) (self.getY() / game.getTrackTileSize());
        int wpX = self.getNextWaypointX();
        int wpY = self.getNextWaypointY();

        int deltaX = wpX - curX;
        int deltaY = wpY - curY;

        //Если не на прямой, то сразу применяем альтернативную тактику
        if (Math.abs(deltaX) > 0 && Math.abs(deltaY) > 0) {

            //Устанавливаем промежуточный WayPoint
            straightTilesCounter = 0;
            path = myMap.findPath(curX, curY, wpX, wpY);
        } else {
            int counterTiles = 0;
            int i = self.getNextWaypointIndex();
            int prevWpX=wpX;
            int prevWpY=wpY;
            //Едем по горизонтали
            if (deltaX != 0) {
                for (; i < world.getWaypoints().length; i++) {
                    int curWpX=world.getWaypoints()[i][0];
                    int curWpY=world.getWaypoints()[i][1];

                    if (Math.abs(curWpY-curY) > 0){
                        straightTilesCounter = Math.abs(prevWpX-curX);
                        path = myMap.findPath(curX, curY, prevWpX, prevWpY);
                        break;
                    }
                    prevWpX=curWpX;
                    prevWpY=curWpY;
                }
            } else {
                for (; i < world.getWaypoints().length; i++) {
                    int curWpX=world.getWaypoints()[i][0];
                    int curWpY=world.getWaypoints()[i][1];

                    if (Math.abs(curWpX-curX) > 0){
                        straightTilesCounter = Math.abs(prevWpY-curY);
                        path = myMap.findPath(curX, curY, prevWpX, prevWpY);
                        break;
                    }
                    prevWpX=curWpX;
                    prevWpY=curWpY;
                }

            }
        }
        if(world.getTick()>1300){
            System.out.println("test");
        }
    }

    public boolean isWayToNextCheckpointStraight(Car self, World world, Game game, Move move) {

        //копируем себе контрольные точки на карте
//        myWayPoints = world.getWaypoints();
        int prevWpX = (int) (self.getX() / game.getTrackTileSize());
        int prevWpY = (int) (self.getY() / game.getTrackTileSize());
        int wpX = self.getNextWaypointX();
        int wpY = self.getNextWaypointY();

        int deltaX = wpX - prevWpX;
        int deltaY = wpY - prevWpY;

        //Если не на прямой, то сразу применяем альтернативную тактику
        if (Math.abs(deltaX) > 0 && Math.abs(deltaY) > 0) {
            //Устанавливаем промежуточный WayPoint
            return false;
        } else {
            //Проверяем есть ли соединение на прямых тайлах

            int counterTiles = 0;
            //Едем по горизонтали
            if (deltaX != 0) {
                int i = wpX > prevWpX ? prevWpX : wpX;
                int iLim = wpX > prevWpX ? wpX : prevWpX;
                int j = prevWpY;


                for (; i < iLim; i++) {
                    TileType type = world.getTilesXY()[i][j];
                    switch (type) {
                        case HORIZONTAL:
                        case CROSSROADS:
                        case TOP_HEADED_T:
                        case BOTTOM_HEADED_T:
                            counterStraightTilesToNextWP++;
                            break;
                        default:
                            return false;
                    }
                }
                return true;

            } else {
                int i = prevWpX;
                int j = wpY > prevWpY ? prevWpY : wpY;
                int jLim = wpY > prevWpY ? wpY : prevWpY;

                for (; j < jLim; j++) {
                    TileType type = world.getTilesXY()[i][j];
                    switch (type) {
                        case VERTICAL:
                        case CROSSROADS:
                        case RIGHT_HEADED_T:
                        case LEFT_HEADED_T:
                            counterStraightTilesToNextWP++;
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }
    }

    public void makeMyWay(Car self, World world, Game game, Move move) {
        myMap = new Map<>(world.getWidth(), world.getHeight(), new ExampleFactory());
        path = new LinkedList<>();

        for (int j = 0; j < world.getHeight(); j++) {
            for (int i = 0; i < world.getWidth(); i++) {
                TileType type = world.getTilesXY()[i][j];
                switch (type){
                    case EMPTY:
                        myMap.getNode(i,j).setWalkable(false);
                        break;
                    case CROSSROADS:
                    case VERTICAL:
                    case HORIZONTAL:
                        myMap.getNode(i,j).sethCosts(1);
                        break;
                    case TOP_HEADED_T:
                    case BOTTOM_HEADED_T:
                    case RIGHT_HEADED_T:
                    case LEFT_HEADED_T:
                        myMap.getNode(i,j).sethCosts(5);
                        break;
                    case UNKNOWN:
                        myMap.getNode(i,j).setWalkable(false);
                        definedMap=false;
                        System.out.println("Мир неопределён");
                        return;

                }
            }
        }

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



//
//    public static void firstCheck(World world) {
//        System.out.println("TILES");
//        for (int i = 0; i < world.getWidth() - 1; i++) {
//            System.out.print(i + "\t\t\t\t\t\t");
//        }
//        System.out.println(world.getWidth() - 1);
//
//        for (int j = 0; j < world.getHeight(); j++) {
//            for (int i = 0; i < world.getWidth() - 1; i++) {
//                TileType type = world.getTilesXY()[i][j];
//                StringBuilder text = new StringBuilder();
//                if (i == 0)
//                    text.append("|");//Добавляем номер строчки, на первом столбце
//
//                text.append(type);
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
//                        if (i == 0)
//                            System.out.print(text + "\t\t");
//                        else
//                            System.out.print(text + "\t\t\t");
//                        break;
//                    default:
//                        System.out.print(text + "\t\t\t");
//                        break;
//
//                }
//            }
//            System.out.println(world.getTilesXY()[world.getWidth() - 1][j]);
//        }
//

//
//    }