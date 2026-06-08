package sk.uniza.adamec2.sem_two.terminal.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.factory.EventFactory;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.event.DetectorEvent;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class DetectorEventFactory implements EventFactory<DetectorEvent, Person> {

    private final ServiceFront<DetectorEvent, Person> serviceFront;
    private final SingleTerminal terminal;

    public DetectorEventFactory(ServiceFront<DetectorEvent, Person> serviceFront, SingleTerminal terminal) {
        this.serviceFront = serviceFront;
        this.terminal = terminal;
    }

    @Override
    public DetectorEvent createEvent(double time, EventCore eventCore, Person entity) {
        return new DetectorEvent(time, eventCore, entity, serviceFront, terminal);
    }
}
