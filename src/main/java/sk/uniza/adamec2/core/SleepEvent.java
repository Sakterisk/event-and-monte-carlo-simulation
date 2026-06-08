package sk.uniza.adamec2.core;

public class SleepEvent extends Event {

    private static final int PLAN_TIME = 250; // ms

    public SleepEvent(EventCore simulation) {
        super(0.0, simulation);
    }

    @Override
    public void execute() {
        if (simulation.getSleepTime() == -1) {
            return;
        }
        try {
            Thread.sleep(simulation.getSleepTime());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        time += PLAN_TIME / (1000.0 * 60.0 * 60.0); // Convert ms to hours
        simulation.addEvent(this);
    }
}
