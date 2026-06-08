package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.Event;
import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.Entity;

public abstract class BaseEvent<E extends Entity> extends Event {

    protected E entity;

    public BaseEvent(double time, EventCore simulation, E entity) {
        super(time, simulation);
        this.entity = entity;
    }

    public E getEntity() {
        return entity;
    }

    public void setEntity(E entity) {
        this.entity = entity;
    }
}
