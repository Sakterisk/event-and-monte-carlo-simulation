package sk.uniza.adamec2.sem_two.terminal.entity;

import sk.uniza.adamec2.sem_two.entity.Entity;

public class Baggage extends Entity {

    public Baggage(int id, int ownerID) {
        int calcID = ownerID * 100 + id;
        super(calcID);
    }

    public int getOwnerID() {
        return id / 100;
    }

    public boolean belongsToOwner(Person person) {
        return getOwnerID() == person.getId();
    }

    @Override
    public String toString() {
        return "Baggage { id=" + id + ", ownerID=" + getOwnerID() +" }";
    }
}
