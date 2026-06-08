package sk.uniza.adamec2.sem_two.entity.factory;

import sk.uniza.adamec2.sem_two.entity.Entity;

public interface EntityFactory<E extends Entity> {
    E createEntity(int id, double arrivalTime);
}
