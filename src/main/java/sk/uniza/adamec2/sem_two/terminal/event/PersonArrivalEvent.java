package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.TestSimulation;
import sk.uniza.adamec2.sem_two.event.ArrivalEvent;
import sk.uniza.adamec2.sem_two.event.BaseEvent;
import sk.uniza.adamec2.sem_two.segment.Arrival;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;

public class PersonArrivalEvent extends ArrivalEvent<Person> {

    public PersonArrivalEvent(double time,
                              EventCore simulation,
                              Person entity,
                              Arrival<? extends ArrivalEvent<Person>, Person> arrival) {
        super(time, simulation, entity, arrival);
    }

    @Override
    protected void afterArrival(Person entity) {
        TerminalSimulation sim = (TerminalSimulation) simulation;
        sim.planArrivalToTerminal(entity);
    }
}
