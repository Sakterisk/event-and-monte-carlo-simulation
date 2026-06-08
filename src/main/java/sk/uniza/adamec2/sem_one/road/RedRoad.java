package sk.uniza.adamec2.sem_one.road;

import sk.uniza.adamec2.generator.DiscreteUniformGen;
import sk.uniza.adamec2.generator.Gen;

import java.util.Random;

public class RedRoad extends Road {

    Gen<Integer> gen;

    public RedRoad(int length) {
        super(length);
        gen = new DiscreteUniformGen(55, 76);
    }

    @Override
    public double getTimeToPass() {
        return (double) getLength() / (double) gen.next();
    }

    @Override
    public double getSlowdownTimeToPass(double slowdownPercent) {
        return (double) getLength() / ((double) gen.next() * ((100.0 - slowdownPercent) / 100.0));
    }
}
