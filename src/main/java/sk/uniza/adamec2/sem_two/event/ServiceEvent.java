package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.TestSimulation;
import sk.uniza.adamec2.sem_two.entity.Entity;
import sk.uniza.adamec2.sem_two.segment.Service;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;

public abstract class ServiceEvent<SE extends ServiceEvent<SE, E>, E extends Entity> extends BaseEvent<E> {

    private final Service<SE, E> service;

    public ServiceEvent(double time, EventCore simulation, E entity, Service<SE, E> service) {
        super(time, simulation, entity);
        this.service = service;
    }

    @Override
    public void execute() {
        E e = service.getEntity();
        service.endService();
        afterServiceEnd(e);
    }

    abstract protected void afterServiceEnd(E entity);
}
