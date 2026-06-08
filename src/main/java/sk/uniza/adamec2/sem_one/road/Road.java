package sk.uniza.adamec2.sem_one.road;

public abstract class Road {

    private final int length;

    public Road(int length) {
        this.length = length;
    }

    public abstract double getTimeToPass();

    public abstract double getSlowdownTimeToPass(double slowdownPercent);

    protected int getLength() {
        return length;
    }
}
