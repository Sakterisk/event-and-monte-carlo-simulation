package sk.uniza.adamec2.sem_two.segment;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.FrontableEntity;
import sk.uniza.adamec2.stat.MeanStat;
import sk.uniza.adamec2.stat.TimeWeightedMeanStat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

public class Front<T extends FrontableEntity> {

    private final Queue<T> queue;
    private int maxSize;
    private final MeanStat averageTime;
    private final TimeWeightedMeanStat averageLength;
    private final EventCore sim;

    public Front(EventCore eventCore) {
        maxSize = 0;
        queue = new ArrayDeque<>();
        sim = eventCore;
        averageTime = new MeanStat();
        averageLength = new TimeWeightedMeanStat();
        averageLength.setInitialValues(0.0, 0.0);
    }

    public Front(EventCore eventCore, int maxSize) {
        this.maxSize = maxSize;
        queue = new ArrayDeque<>(maxSize);
        sim = eventCore;
        averageTime = new MeanStat();
        averageLength = new TimeWeightedMeanStat();
        averageLength.setInitialValues(0.0, 0.0);
    }

    public void add(T entity) {
        if (maxSize > 0 && queue.size() >= maxSize) {
            throw new IllegalStateException("Front is full. Cannot add more entities.");
        }
        queue.add(entity);
        entity.setFrontArrivalTime(sim.getTime());
        averageLength.add(queue.size(), sim.getTime());
    }

    public T poll() {
        if (queue.isEmpty()) {
            return null;
        }
        T entity = queue.poll();
        averageTime.add(sim.getTime() - entity.getFrontArrivalTime());
        averageLength.add(queue.size(), sim.getTime());
        return entity;
    }

    public T peek() {
        if (queue.isEmpty()) {
            return null;
        }
        return queue.peek();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public void addZeroWaitTime() {
        averageTime.add(0.0);
    }

    public ArrayList<T> getAll() {
        return new ArrayList<>(queue);
    }

    public double averageTime() {
        return averageTime.getMean();
    }

    public double averageLength() {
        return averageLength.getMean();
    }

    public int freeSpace() {
        if (maxSize == 0) {
            return Integer.MAX_VALUE;
        }
        return maxSize - queue.size();
    }

    public void clear() {
        queue.clear();
        averageLength.clear();
        averageTime.clear();
        averageLength.setInitialValues(0.0, 0.0);
    }

    public void changeMaxSize(int maxSize) {
        if (this.maxSize != maxSize) {
            clear();
            this.maxSize = maxSize;
        }
    }
}
