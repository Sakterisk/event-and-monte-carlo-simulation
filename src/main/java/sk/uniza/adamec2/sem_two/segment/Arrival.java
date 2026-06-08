package sk.uniza.adamec2.sem_two.segment;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.sem_two.entity.Entity;
import sk.uniza.adamec2.sem_two.entity.factory.EntityFactory;
import sk.uniza.adamec2.sem_two.event.ArrivalEvent;

public class Arrival<AE extends ArrivalEvent<E>, E extends Entity> {

    private final EntityFactory<E> entityFactory;
    private Gen<Double> arrivalGen;
    private AE event;
    private final EventCore sim;
    private int nextID = 1;
    private E willArrive;
    private double arrivalTime;

    public Arrival(Gen<Double> arrivalGen, EntityFactory<E> entityFactory, EventCore eventCore) {
        this.arrivalGen = arrivalGen;
        this.entityFactory = entityFactory;
        this.sim = eventCore;
    }

    public void planNextArrivalEvent() {
        arrivalTime = sim.getTime() + arrivalGen.next();
        E entity = entityFactory.createEntity(nextID, arrivalTime);
        willArrive = entity;
        nextID++;
        if (nextID > 1_000_000) {
            nextID = 1;
        }
        event.setEntity(entity);
        event.setTime(arrivalTime);
        sim.addEvent(event);
    }

    public E getWillArrive() {
        return willArrive;
    }

    public double getNextArrivalTime() {
        return arrivalTime;
    }

    public void clear() {
        arrivalTime = 0;
        willArrive = null;
        nextID = 1;
    }

    public void addArrivalEvent(AE arrivalEvent) {
        this.event = arrivalEvent;
    }

    public void setArrivalGen(Gen<Double> arrivalGen) {
        this.arrivalGen = arrivalGen;
    }
}
