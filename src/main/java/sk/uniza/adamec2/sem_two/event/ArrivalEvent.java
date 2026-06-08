package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.Entity;
import sk.uniza.adamec2.sem_two.segment.Arrival;

public abstract class ArrivalEvent<E extends Entity> extends BaseEvent<E> {

    private final Arrival<? extends ArrivalEvent<E>, E> arrival;

    public ArrivalEvent(double time, EventCore simulation, E entity, Arrival<? extends ArrivalEvent<E>, E> arrival) {
        super(time, simulation, entity);
        this.arrival = arrival;
    }

    @Override
    public void execute() {
        E e = entity;
        arrival.planNextArrivalEvent();
        afterArrival(e);
    }

    abstract protected void afterArrival(E entity);
}
