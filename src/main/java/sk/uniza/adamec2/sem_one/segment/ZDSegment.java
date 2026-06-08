package sk.uniza.adamec2.sem_one.segment;

import sk.uniza.adamec2.sem_one.road.GreenRoad;
import sk.uniza.adamec2.sem_one.road.RedRoad;

import java.util.Random;

public class ZDSegment extends Segment {

    private final RedRoad redRoad1;
    private final GreenRoad greenRoad1;

    public ZDSegment() {
        this.redRoad1 = new RedRoad(4);
        this.greenRoad1 = new GreenRoad(4);
    }

    @Override
    public double getBestTimeToPass() {
        return Math.min(redRoad1.getTimeToPass(), greenRoad1.getTimeToPass());
    }
}
