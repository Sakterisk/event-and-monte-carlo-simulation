package sk.uniza.adamec2.sem_two.segment;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.sem_two.entity.FrontableEntity;
import sk.uniza.adamec2.sem_two.event.*;

public class ServiceFront<SE extends ServiceFrontEvent<SE, E>, E extends FrontableEntity> extends Service<SE, E> {

    private final Front<E> front;
    private int willArrive;

    public ServiceFront(Front<E> front, Gen<Double> serviceTimeGen, EventCore eventCore) {
        super(serviceTimeGen, eventCore);
        this.front = front;
        this.willArrive = 0;
    }

    public void planNewEntityArrival(E entity) {
        willArrive++;
        sim.addEvent(eventFactory.createEvent(sim.getTime(), sim, entity));
    }

    public void tryToStartService() {
        if (endServiceTime >= sim.getTime()) {
            return;
        }
        if (getEntity() != null) {
            return;
        }
        sim.addEvent(eventFactory.createEvent(sim.getTime(), sim, null));
    }

    public void addToFront(E entity) {
        front.add(entity);
        willArrive--;
    }

    public E pollFront() {
        return front.poll();
    }

    public int getFreeSpaceFront() {
        return front.freeSpace() - willArrive;
    }

    public double averageQueueLength() {
        return front.averageLength();
    }

    public double averageQueueWaitTime() {
        return front.averageTime();
    }

    public boolean isEmptyFront() {
        return front.isEmpty();
    }

    public void addZeroWaitTimeFront() {
        front.addZeroWaitTime();
    }

    public Front<E> getFront() {
        return front;
    }

    public void willArriveMinus() {
        willArrive--;
    }

    public void clear() {
        super.clear();
        front.clear();
        willArrive = 0;
    }

    public void changeFrontMaxSize(int maxSize) {
        front.changeMaxSize(maxSize);
    }
}
