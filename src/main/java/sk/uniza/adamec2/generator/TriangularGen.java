package sk.uniza.adamec2.generator;

import java.util.Random;

public class TriangularGen implements Gen<Double> {

    private final double min;
    private final double max;
    private final double mode;
    private final Random gen;

    public TriangularGen(double min, double max, double mode) {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max.");
        }
        if (mode < min || mode > max) {
            throw new IllegalArgumentException("Mode must be between min and max.");
        }
        this.min = min;
        this.max = max;
        this.mode = mode;
        this.gen = new Random(SeedGen.nextSeed());
    }

    @Override
    public Double next() {
        double uniform = gen.nextDouble();
        double side = (mode - min) / (max - min);
        if (uniform < side) {
            return min + Math.sqrt(uniform * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1 - uniform) * (max - min) * (max - mode));
        }
    }
}
