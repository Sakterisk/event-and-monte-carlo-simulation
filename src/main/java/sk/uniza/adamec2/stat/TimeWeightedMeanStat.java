package sk.uniza.adamec2.stat;

public class TimeWeightedMeanStat {

    private double sum;
    private double lastValue;
    private double lastTime;
    private double totalTime;

    public TimeWeightedMeanStat() {
        clear();
    }

    public void add(double value, double time) {
        double deltaTime = time - lastTime;

        sum += lastValue * deltaTime;
        totalTime += deltaTime;
        lastValue = value;
        lastTime = time;
    }

    public double getMean() {
        return totalTime > 0.0 ? sum / totalTime : 0.0;
    }

    public void setInitialValues(double initialValue, double initialTime) {
        lastTime = initialTime;
        lastValue = initialValue;
        sum = 0.0;
        totalTime = 0.0;
    }

    public void clear() {
        sum = 0.0;
        lastTime = 0.0;
        lastValue = 0.0;
        totalTime = 0.0;
    }
}