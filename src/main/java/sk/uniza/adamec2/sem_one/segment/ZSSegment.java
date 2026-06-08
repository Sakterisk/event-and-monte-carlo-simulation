package sk.uniza.adamec2.sem_one.segment;

import sk.uniza.adamec2.sem_one.road.BlackRoad;
import sk.uniza.adamec2.sem_one.road.GreenRoad;
import sk.uniza.adamec2.sem_one.road.RedRoad;

import java.util.Random;

public class ZSSegment extends Segment {

    private final RedRoad redRoad1;
    private final RedRoad redRoad2;
    private final BlackRoad blackRoad1;
    private final GreenRoad greenRoad1;

    public ZSSegment() {
        redRoad1 = new RedRoad(3);
        redRoad2 = new RedRoad(4);
        blackRoad1 = new BlackRoad(3);
        greenRoad1 = new GreenRoad(4);
    }

    @Override
    public double getBestTimeToPass() {
        double firstOption = redRoad1.getTimeToPass() + redRoad2.getTimeToPass();
        double secondOption = blackRoad1.getTimeToPass() + greenRoad1.getTimeToPass();
        return Math.min(firstOption, secondOption);
    }
}
