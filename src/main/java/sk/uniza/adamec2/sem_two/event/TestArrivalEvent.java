package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.TestSimulation;
import sk.uniza.adamec2.sem_two.entity.Customer;
import sk.uniza.adamec2.sem_two.segment.Arrival;

public class TestArrivalEvent extends ArrivalEvent<Customer> {
    public TestArrivalEvent(double time, EventCore simulation, Customer entity, Arrival<? extends ArrivalEvent<Customer>, Customer> arrival) {
        super(time, simulation, entity, arrival);
    }

    @Override
    protected void afterArrival(Customer entity) {
        TestSimulation sim = (TestSimulation) simulation;
        sim.planArrivalToService(entity);
    }
}
