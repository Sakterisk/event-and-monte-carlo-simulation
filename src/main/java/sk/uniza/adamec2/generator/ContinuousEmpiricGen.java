package sk.uniza.adamec2.generator;

import java.util.ArrayList;
import java.util.Random;

public class ContinuousEmpiricGen implements Gen<Double> {

    private record InnerGen(double from, double to, Random gen) {
        public double next() {
            return gen.nextDouble(from, to);
        }
    }

    public record InputWrapper(double from, double to, double probability) {
    }

    private final ArrayList<InnerGen> innerGens = new ArrayList<InnerGen>();
    private final ArrayList<Double> cumulativeProbabilities = new ArrayList<>();
    private final Random probabilityGen;

    public ContinuousEmpiricGen(InputWrapper... rangeAndProbability) {
        probabilityGen = new Random(SeedGen.nextSeed());
        double currProbSum = 0;
        for (InputWrapper input : rangeAndProbability) {
            currProbSum += input.probability();
            cumulativeProbabilities.add(currProbSum);
            innerGens.add(new InnerGen(input.from(), input.to(), new Random(SeedGen.nextSeed())));
        }
        if (Math.abs(currProbSum - 1.0) > 0.000001) {
            throw new IllegalArgumentException("Probabilities must sum to 1.");
        }
    }

    @Override
    public Double next() {
        double prob = probabilityGen.nextDouble();
        for (int i = 0; i < cumulativeProbabilities.size(); i++) {
            if (prob < cumulativeProbabilities.get(i)) {
                return innerGens.get(i).next();
            }
        }
        throw new RuntimeException("Problem with generating the probability.");
    }
}
