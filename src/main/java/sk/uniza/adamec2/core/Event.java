package sk.uniza.adamec2.core;

abstract public class Event implements Comparable<Event> {

    protected double time;
    protected final EventCore simulation;

    public Event(double time, EventCore simulation) {
        this.time = time;
        this.simulation = simulation;
    }

    @Override
    public int compareTo(Event other) {
        return Double.compare(this.time, other.time);
    }

    abstract public void execute();

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
