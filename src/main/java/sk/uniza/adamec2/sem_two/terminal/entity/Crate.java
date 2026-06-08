package sk.uniza.adamec2.sem_two.terminal.entity;

import sk.uniza.adamec2.sem_two.entity.FrontableEntity;

public class Crate extends FrontableEntity {

    private final Baggage baggage;

    public Crate(Baggage baggage) {
        super(baggage.getId());
        this.baggage = baggage;
    }

    public Baggage getBaggage() {
        return baggage;
    }

    @Override
    public String toString() {
        return "Crate {\n" + "\tid=" + id + ",\n\tbaggage= " + baggage + "\n}";
    }
}
