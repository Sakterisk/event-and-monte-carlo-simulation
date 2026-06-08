package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.TestSimulation;
import sk.uniza.adamec2.sem_two.entity.Customer;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;

public class TestServiceFrontEvent extends ServiceFrontEvent<TestServiceFrontEvent, Customer> {

    public TestServiceFrontEvent(double time, EventCore simulation, Customer entity, ServiceFront<TestServiceFrontEvent, Customer> serviceFront) {
        super(time, simulation, entity, serviceFront);
    }

    @Override
    protected void afterServiceEnd(Customer entity) {
        TestSimulation sim = (TestSimulation) simulation;
        sim.addAverageTimeInSim(sim.getTime() - entity.getArrivalTime());
        System.out.println("Service ended for " + entity);
    }

    @Override
    protected void afterStartService() {

    }
}
