package sk.uniza.adamec2.sem_two.terminal.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.factory.EventFactory;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.event.PlaceCrateEvent;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class PlaceCrateEventFactory implements EventFactory<PlaceCrateEvent, Person> {

    private final Front<Person> waitingFront;
    private final SingleTerminal terminal;

    public PlaceCrateEventFactory(Front<Person> waitingFront, SingleTerminal terminal) {
        this.waitingFront = waitingFront;
        this.terminal = terminal;
    }


    @Override
    public PlaceCrateEvent createEvent(double time, EventCore eventCore, Person entity) {
        return new PlaceCrateEvent(time, eventCore, entity, waitingFront, terminal);
    }
}
