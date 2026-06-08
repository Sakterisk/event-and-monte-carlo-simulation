package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.BaseEvent;
import sk.uniza.adamec2.sem_two.event.ServiceFrontEvent;
import sk.uniza.adamec2.sem_two.segment.Service;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class DetectorEvent extends ServiceFrontEvent<DetectorEvent, Person> {

    private SingleTerminal terminal;

    public DetectorEvent(double time,
                         EventCore simulation,
                         Person entity,
                         Service<DetectorEvent, Person> serviceFront,
                         SingleTerminal terminal
    ) {
        super(time, simulation, entity, serviceFront);
        this.terminal = terminal;
    }

    @Override
    protected void afterServiceEnd(Person entity) {
        if (entity.getNumberOfBaggage() == 0) {
            terminal.personExit(entity);
        } else {
            terminal.personTransitionToTakeCrate(entity);
        }
    }

    @Override
    protected void afterStartService() {

    }
}
