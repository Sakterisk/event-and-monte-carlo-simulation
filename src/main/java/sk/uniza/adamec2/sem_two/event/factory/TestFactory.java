package sk.uniza.adamec2.sem_two.event.factory;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.Customer;
import sk.uniza.adamec2.sem_two.event.TestServiceFrontEvent;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;

public class TestFactory implements EventFactory<TestServiceFrontEvent, Customer> {

    private final ServiceFront<TestServiceFrontEvent, Customer> serviceFront;

    public TestFactory(ServiceFront<TestServiceFrontEvent, Customer> serviceFront) {
        this.serviceFront = serviceFront;
    }

    @Override
    public TestServiceFrontEvent createEvent(double time, EventCore eventCore, Customer entity) {
        return new TestServiceFrontEvent(time, eventCore, entity, serviceFront);
    }
}
