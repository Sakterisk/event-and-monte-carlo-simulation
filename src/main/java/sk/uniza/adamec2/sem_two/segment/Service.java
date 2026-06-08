package sk.uniza.adamec2.sem_two.segment;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.sem_two.entity.Entity;
import sk.uniza.adamec2.sem_two.event.factory.EventFactory;
import sk.uniza.adamec2.sem_two.event.ServiceEvent;

public class Service<SE extends ServiceEvent<SE, E>, E extends Entity> {

    protected final EventCore sim;
    protected EventFactory<SE, E> eventFactory;
    private E entity;
    private boolean available;
    private final Gen<Double> serviceTimeGen;
    protected double endServiceTime;

    public Service(Gen<Double> serviceTimeGen, EventCore eventCore) {
        this.sim = eventCore;
        available = true;
        this.serviceTimeGen = serviceTimeGen;
    }

    public void setEventFactory(EventFactory<SE, E> eventFactory) {
        this.eventFactory = eventFactory;
    }

    public void startService(E entity) {
        this.entity = entity;
        endServiceTime = sim.getTime() + serviceTimeGen.next();
        available = false;
        sim.addEvent(eventFactory.createEvent(endServiceTime, sim, null));
    }

    public E getEntity() {
        return entity;
    }

    public void endService() {
        entity = null;
        available = true;
        endServiceTime = 0.0;
    }

    public double getEndServiceTime() {
        return endServiceTime;
    }

    public boolean isAvailable() {
        return available;
    }

    public void clear() {
        available = true;
        entity = null;
        endServiceTime = 0.0;
    }
}
