package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.BaseEvent;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class PlaceCrateEvent extends BaseEvent<Person> {

    private final Front<Person> waitingFront;
    private final SingleTerminal terminal;

    public PlaceCrateEvent(double time,
                           EventCore simulation,
                           Person entity,
                           Front<Person> waitingFront,
                           SingleTerminal terminal) {
        super(time, simulation, entity);
        this.waitingFront = waitingFront;
        this.terminal = terminal;
    }

    @Override
    public void execute() {
        TerminalSimulation sim = (TerminalSimulation) simulation;
        if (entity != null) {
            waitingFront.add(entity);
            sim.lengthBeforePlaceCrateChange();
            if (waitingFront.size() == 1) {
                terminal.tryPlanPlaceCrateEvent();
            }
        } else {
            if (waitingFront.isEmpty()) {
                return;
            }
            if (waitingFront.peek().getNumberOfBaggage() == 0) {
                Person curr = waitingFront.poll();
                sim.lengthBeforePlaceCrateChange();
                terminal.personTransitionToDetector(curr);
                terminal.tryPlanPlaceCrateEvent();
                return;
            }
            if (terminal.freeSpaceBeforeXray() == 0) {
                return;
            }

            Person curr = waitingFront.peek();
            int amountOfBaggage = Math.min(terminal.freeSpaceBeforeXray(), curr.getNumberOfHoldingBaggage());
            for (int i = 0; i < amountOfBaggage; i++) {
                terminal.placeCrateBeforeXray(curr.putBaggageInCrate());
            }
            if (curr.getNumberOfHoldingBaggage() == 0) {
                waitingFront.poll();
                sim.lengthBeforePlaceCrateChange();
                terminal.personTransitionToDetector(curr);
                terminal.tryPlanPlaceCrateEvent();
            }
        }
    }
}
