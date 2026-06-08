package sk.uniza.adamec2.sem_two.terminal;

import sk.uniza.adamec2.generator.*;

import java.util.Random;

public class DetectorGen implements Gen<Double> {

    private final ContinuousUniformGen mainGen;
    private final DiscreteEmpiricGen probGen;
    private final TriangularGen personalCheckGen;

    public DetectorGen() {
        this.mainGen = new ContinuousUniformGen(6.0 / 3600.0, 27.0 / 3600.0);
        this.probGen = new DiscreteEmpiricGen(
                new DiscreteEmpiricGen.InputWrapper(0, 1, 0.81),
                new DiscreteEmpiricGen.InputWrapper(1, 2, 0.19)
        );
        this.personalCheckGen = new TriangularGen(10.0 / 3600.0, 120.0 / 3600.0, 35.0 / 3600.0);
    }

    @Override
    public Double next() {
        double total = mainGen.next();
        int personalCheck = probGen.next();
        if (personalCheck == 1) {
            total += personalCheckGen.next();
        }
        return total;
    }
}
