package sk.uniza.adamec2.sem_two.entity;

public abstract class Entity {

    protected int id;

    public Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
