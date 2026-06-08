package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.Event;
import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.BaseEvent;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;

public class WarmUpEndEvent extends Event {
    public WarmUpEndEvent(double time, EventCore simulation) {
        super(time, simulation);
    }

    @Override
    public void execute() {
        TerminalSimulation sim = (TerminalSimulation) simulation;
        sim.resetStatistics();
    }
}
