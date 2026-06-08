package sk.uniza.adamec2.stat;

public class MeanStat {

    private static final int MIN_COUNT_FOR_VARIANCE = 30;
    private static final double T_ALFA = 1.96;

    private double sum;
    private double sumSquared;
    private int count;

    public MeanStat() {
        this.sum = 0.0;
        this.count = 0;
    }

    public void add(double value) {
        sum += value;
        sumSquared += value * value;
        count++;
    }

    public double getMean() {
        return count > 0 ? sum / count : 0.0;
    }

    public double getVariance() {
        return count >= 30 ? (Math.sqrt((sumSquared - ((sum * sum) / count)) / (count - 1)) * T_ALFA) / Math.sqrt(count) : -1.0;
    }

    public void clear() {
        sum = 0.0;
        count = 0;
    }
}
