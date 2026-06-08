package sk.uniza.adamec2.core;

import sk.uniza.adamec2.gui.SimulationObserver;

import java.util.ArrayList;
import java.util.PriorityQueue;

abstract public class EventCore extends MonteCarloCore {

    protected double time;
    protected double maxTime;
    private final PriorityQueue<Event> queue = new PriorityQueue<>();
    private boolean isPaused;
    private int sleepTime;
    private final SleepEvent sleepEvent;
    private final ArrayList<SimulationObserver> observers;

    public EventCore(double maxTime) {
        sleepEvent = new SleepEvent(this);
        observers = new ArrayList<>();
        this.maxTime = maxTime;
        sleepTime = 0;
        isPaused = false;
    }

    @Override
    protected void beforeSimulation(int numberOfReplications) {
    }

    @Override
    protected void beforeReplication(int replicationNumber) {
        isPaused = false;
        if (sleepTime >= 0) {
            sleepEvent.setTime(0.0);
            addEvent(sleepEvent);
        }
    }

    @Override
    protected void afterReplication(int replicationNumber) {
        updateGUI();
        queue.clear();
    }

    @Override
    protected void afterSimulation(int numberOfCompletedReplications) {
        updateGUI();
    }

    @Override
    protected void simulateReplication(int replicationNumber) {
        while (!queue.isEmpty() && isSimulationRunning) {
            while (isPaused) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Event event = queue.peek();
            time = event.getTime();
            if (time > maxTime) {
                break;
            }
            queue.poll();
            event.execute();
            if (sleepTime >= 0) {
                updateGUI();
            }
        }
    }

    private void updateGUI() {
        for (SimulationObserver observer : observers) {
            observer.refresh(this);
        }
    }

    public void addEvent(Event event) {
        if (event.getTime() < time) {
            throw new IllegalArgumentException("Event time exceeds maximum simulation time.");
        }
        queue.add(event);
    }

    public int getSleepTime()   {
        return sleepTime;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public void subscribe(SimulationObserver observer) {
        observers.add(observer);
    }

    public void unsubscribe(SimulationObserver observer) {
        observers.remove(observer);
    }

    public void changeSleepTime(int sleepTime) {
        if (sleepTime >= 0 && this.sleepTime == -1) {
            sleepEvent.setTime(time);
            addEvent(sleepEvent);
        }
        this.sleepTime = sleepTime;
    }

    public double getTime() {
        return time;
    }
}
