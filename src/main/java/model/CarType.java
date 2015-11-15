package model;

/**
 * Тип кодемобиля.
 * <p/>
 * В Раунде 1 чемпионата стратегия игрока управляет одним кодемобилем типа {@code BUGGY}.
 * В гонке участвуют {@code 4} игрока.
 * <p/>
 * В Раунде 2 чемпионата стратегия игрока управляет одним кодемобилем типа {@code JEEP}.
 * В гонке участвуют {@code 4} игрока.
 * <p/>
 * В Финале в распоряжении стратегии игрока находится по одному кодемобилю каждого типа.
 * В гонке участвуют {@code 2} игрока.
 */
public enum CarType {
    /**
     * Багги. Стреляет тремя небольшими шайбами, расходящимися под небольшим углом. Немного легче джипа.
     */
    BUGGY,

    /**
     * Джип. Стреляет большими массивными шинами, отскакивающими от машин и гранци трассы. Немного тяжелее багги.
     */
    JEEP
}
