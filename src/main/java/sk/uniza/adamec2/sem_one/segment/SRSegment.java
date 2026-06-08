package sk.uniza.adamec2.sem_one.segment;

import sk.uniza.adamec2.sem_one.road.BlackRoad;
import sk.uniza.adamec2.sem_one.road.BlueRoad;

public class SRSegment extends Segment {

    private final BlueRoad blueRoad1;
    private final BlueRoad blueRoad2;
    private final BlackRoad blackRoad1;

    public SRSegment() {
        blueRoad1 = new BlueRoad(8);
        blueRoad2 = new BlueRoad(5);
        blackRoad1 = new BlackRoad(5);
    }

    @Override
    public double getBestTimeToPass() {
        double blueRoad1Time = blueRoad1.getTimeToPass();
        double blueRoad2Time = blueRoad2.getTimeToPass();
        double blackRoadTime = blackRoad1.getTimeToPass();
        return blueRoad1Time + Math.min(blueRoad2Time, blackRoadTime);
    }
}
