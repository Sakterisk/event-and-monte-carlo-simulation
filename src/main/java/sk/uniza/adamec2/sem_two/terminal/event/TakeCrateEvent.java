package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.BaseEvent;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class TakeCrateEvent extends BaseEvent<Person> {

    private Front<Person> waitingPeople;
    private Front<Crate> availableCrates;
    private SingleTerminal terminal;

    public TakeCrateEvent(double time,
                          EventCore simulation,
                          Person entity,
                          Front<Person> waitingPeople,
                          Front<Crate> availableCrates,
                          SingleTerminal terminal) {
        super(time, simulation, entity);
        this.waitingPeople = waitingPeople;
        this.availableCrates = availableCrates;
        this.terminal = terminal;
    }

    @Override
    public void execute() {
        TerminalSimulation sim = (TerminalSimulation) simulation;
        if (entity != null) {
            waitingPeople.add(entity);
            sim.lengthBeforeTakeCrateChange();
            if (waitingPeople.size() == 1) {
                terminal.tryPlanTakeCrateEvent();
            }
        } else {
            if (waitingPeople.isEmpty() || availableCrates.isEmpty()) {
                return;
            }

            Person curr = waitingPeople.peek();
            int amountOfCrates = Math.min(curr.getNumberOfBaggage() - curr.getNumberOfHoldingBaggage(),
                    availableCrates.size());
            for (int i = 0; i < amountOfCrates; i++) {
                Crate crate = availableCrates.poll();
                curr.takeBaggageFromCrate(crate);
            }
            if (amountOfCrates > 0) {
                terminal.tryStartXrayServiceEvent();
            }
            if (curr.getNumberOfHoldingBaggage() == curr.getNumberOfBaggage()) {
                waitingPeople.poll();
                sim.lengthBeforeTakeCrateChange();
                terminal.personExit(curr);
                terminal.tryPlanTakeCrateEvent();
            }
        }
    }
}
