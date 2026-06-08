package sk.uniza.adamec2.generator;

import java.util.Random;

public class ContinuousUniformGen implements Gen<Double> {

    private final double min;
    private final double max;
    private final Random gen;

    public ContinuousUniformGen(double min, double max) {
        this.min = min;
        this.max = max;
        this.gen = new Random(SeedGen.nextSeed());
    }

    @Override
    public Double next() {
        return gen.nextDouble(min, max);
    }
}
