package sk.uniza.adamec2.sem_two.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.Entity;
import sk.uniza.adamec2.sem_two.event.BaseEvent;

public interface EventFactory<BE extends BaseEvent<E>, E extends Entity> {
    BE createEvent(double time, EventCore eventCore, E entity);
}
