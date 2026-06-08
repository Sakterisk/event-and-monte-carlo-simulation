package sk.uniza.adamec2.sem_two.terminal.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.factory.EventFactory;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.event.TakeCrateEvent;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class TakeCrateEventFactory implements EventFactory<TakeCrateEvent, Person> {

    private final Front<Person> waitingPeople;
    private final Front<Crate> availableCrates;
    private final SingleTerminal terminal;

    public TakeCrateEventFactory(Front<Person> waitingPeople, Front<Crate> availableCrates, SingleTerminal terminal) {
        this.waitingPeople = waitingPeople;
        this.availableCrates = availableCrates;
        this.terminal = terminal;
    }

    @Override
    public TakeCrateEvent createEvent(double time, EventCore eventCore, Person entity) {
        return new TakeCrateEvent(time, eventCore, entity, waitingPeople, availableCrates, terminal);
    }
}
