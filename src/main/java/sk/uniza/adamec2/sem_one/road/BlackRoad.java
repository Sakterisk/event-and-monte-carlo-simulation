package sk.uniza.adamec2.sem_one.road;

import sk.uniza.adamec2.generator.ContinuousEmpiricGen;

import java.util.Random;

public class BlackRoad extends Road{

    private ContinuousEmpiricGen gen;

    public BlackRoad(int length) {
        super(length);
        gen = new ContinuousEmpiricGen(
                new ContinuousEmpiricGen.InputWrapper(10.0, 20.0, 0.1),
                new ContinuousEmpiricGen.InputWrapper(20.0, 32.0, 0.5),
                new ContinuousEmpiricGen.InputWrapper(32.0, 45.0, 0.2),
                new ContinuousEmpiricGen.InputWrapper(45.0, 75.0, 0.15),
                new ContinuousEmpiricGen.InputWrapper(75.0, 85.0, 0.05)
        );
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
