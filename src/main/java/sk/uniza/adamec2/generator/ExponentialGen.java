package sk.uniza.adamec2.generator;

import java.util.Random;

public class ExponentialGen implements Gen<Double> {

    private final double lambda;
    private final Random gen;

    public ExponentialGen(double lambda) {
        this.lambda = lambda;
        this.gen = new Random(SeedGen.nextSeed());
    }

    @Override
    public Double next() {
        double uniform = gen.nextDouble();
        return -Math.log(1 - uniform) / lambda;
    }
}
