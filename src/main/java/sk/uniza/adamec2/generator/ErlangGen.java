package sk.uniza.adamec2.generator;

import java.util.Random;

public class ErlangGen implements Gen<Double> {

    private final int k;
    private final double lambda;
    private final Random gen;

    /**
     * Erlang(k, lambda) generátor medzidobí príchodov sanitky.
     *
     * @param k      tvar (počet fáz) — pre sanitku k=7
     * @param lambda miera každej fázy — pre sanitku lambda = 1/50.15 ≈ 0.01994
     */
    public ErlangGen(int k, double lambda) {
        if (k <= 0) throw new IllegalArgumentException("k must be positive");
        if (lambda <= 0) throw new IllegalArgumentException("lambda must be positive");
        this.k = k;
        this.lambda = lambda;
        this.gen = new Random(SeedGen.nextSeed());
    }

    @Override
    public Double next() {
        double sum = 0.0;
        for (int i = 0; i < k; i++) {
            double uniform = gen.nextDouble();
            sum += -Math.log(1 - uniform) / lambda;
        }
        return sum;
    }
}