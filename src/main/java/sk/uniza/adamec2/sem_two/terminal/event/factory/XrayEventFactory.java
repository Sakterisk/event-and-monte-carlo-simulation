package sk.uniza.adamec2.sem_two.terminal.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.event.factory.EventFactory;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.event.XrayEvent;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;

public class XrayEventFactory implements EventFactory<XrayEvent, Crate> {

    private final ServiceFront<XrayEvent, Crate> xrayServiceFront;
    private final SingleTerminal terminal;

    public XrayEventFactory(ServiceFront<XrayEvent, Crate> serviceFront, SingleTerminal terminal) {
        this.xrayServiceFront = serviceFront;
        this.terminal = terminal;
    }

    @Override
    public XrayEvent createEvent(double time, EventCore eventCore, Crate entity) {
        return new XrayEvent(time, eventCore, entity, xrayServiceFront, terminal);
    }
}
