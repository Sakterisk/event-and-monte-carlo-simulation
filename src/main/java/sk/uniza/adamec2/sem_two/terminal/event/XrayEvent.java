package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.ServiceFrontEvent;
import sk.uniza.adamec2.sem_two.segment.Service;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class XrayEvent extends ServiceFrontEvent<XrayEvent, Crate> {

    private final SingleTerminal terminal;

    public XrayEvent(double time,
                     EventCore simulation,
                     Crate entity, Service<XrayEvent, Crate> serviceFront,
                     SingleTerminal terminal) {
        super(time, simulation, entity, serviceFront);
        this.terminal = terminal;
    }

    @Override
    protected void afterServiceEnd(Crate entity) {
        terminal.addCrateToFrontAfterXray(entity);
        terminal.tryPlanTakeCrateEvent();
        terminal.tryPlanPlaceCrateEvent();
    }

    @Override
    protected boolean conditionStartService() {
        return !terminal.isAfterXrayFull();
    }

    @Override
    protected void afterStartService() {
        terminal.tryPlanPlaceCrateEvent();
    }
}
