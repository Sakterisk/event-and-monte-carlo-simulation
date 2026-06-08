package sk.uniza.adamec2.sem_one.segment;

import sk.uniza.adamec2.sem_one.road.BlackRoad;
import sk.uniza.adamec2.sem_one.road.BlueRoad;
import sk.uniza.adamec2.sem_one.road.RedRoad;

import java.util.Random;

public class DRSegment extends Segment {

    private final BlueRoad blueRoad1;
    private final BlueRoad blueRoad2;
    private final RedRoad redRoad1;
    private final RedRoad redRoad2;
    private final BlackRoad blackRoad1;

    public DRSegment() {
        blueRoad1 = new BlueRoad(1);
        blueRoad2 = new BlueRoad(1);
        redRoad1 = new RedRoad(3);
        redRoad2 = new RedRoad(2);
        blackRoad1 = new BlackRoad(1);
    }

    @Override
    public double getBestTimeToPass() {
        double blueRoad1Time = blueRoad1.getTimeToPass();
        double blueRoad2Time = blueRoad2.getTimeToPass();
        double redRoad1Time = redRoad1.getTimeToPass();
        double redRoad2Time = redRoad2.getTimeToPass();
        double blackRoad1Time = blackRoad1.getTimeToPass();
        return blueRoad1Time + blackRoad1Time + Math.min(redRoad1Time, redRoad2Time + blueRoad2Time);
    }
}
