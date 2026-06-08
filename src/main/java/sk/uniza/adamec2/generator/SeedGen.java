package sk.uniza.adamec2.generator;

import java.util.Random;

public class SeedGen {
    private static Random rand;

    public static long nextSeed() {
        if (rand == null) {
            rand = new Random();
        }
        return rand.nextLong();
    }
}
