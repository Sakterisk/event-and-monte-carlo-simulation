package sk.uniza.adamec2.generator;

import java.util.Random;

public class DiscreteUniformGen implements Gen<Integer> {

    private final int min;
    private final int max;
    private final Random gen;

    public DiscreteUniformGen(int min, int max) {
        this.min = min;
        this.max = max;
        this.gen = new Random(SeedGen.nextSeed());
    }

    @Override
    public Integer next() {
        return gen.nextInt(min, max);
    }
}
