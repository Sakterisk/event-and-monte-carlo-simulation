package sk.uniza.adamec2.sem_one.road;

import sk.uniza.adamec2.generator.DiscreteEmpiricGen;

import java.util.Random;

public class BlueRoad extends Road {

    private DiscreteEmpiricGen gen;

    public BlueRoad(int length) {
        super(length);
        this.gen = new DiscreteEmpiricGen(
                new DiscreteEmpiricGen.InputWrapper(15, 29, 0.2),
                new DiscreteEmpiricGen.InputWrapper(29, 45, 0.4),
                new DiscreteEmpiricGen.InputWrapper(45, 65, 0.4)
        );
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
