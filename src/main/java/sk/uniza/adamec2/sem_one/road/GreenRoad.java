package sk.uniza.adamec2.sem_one.road;

import sk.uniza.adamec2.generator.ContinuousUniformGen;
import sk.uniza.adamec2.generator.Gen;

import java.util.Random;

public class GreenRoad extends Road {

    private Gen<Double> gen;

    public GreenRoad(int length) {
        super(length);
        gen = new ContinuousUniformGen(50.0, 80.0);
    }

    @Override
    public double getTimeToPass() {
        return (double) getLength() / gen.next();
    }

    @Override
    public double getSlowdownTimeToPass(double slowdownPercent) {
        return (double) getLength() / (gen.next() * ((100.0 - slowdownPercent) / 100.0));
    }
}
