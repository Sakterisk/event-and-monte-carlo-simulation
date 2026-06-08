package sk.uniza.adamec2.sem_two;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.generator.ExponentialGen;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.generator.TriangularGen;
import sk.uniza.adamec2.sem_two.entity.Customer;
import sk.uniza.adamec2.sem_two.entity.factory.CustomerFactory;
import sk.uniza.adamec2.sem_two.event.*;
import sk.uniza.adamec2.sem_two.event.factory.TestFactory;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.segment.Arrival;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.stat.MeanStat;
import sk.uniza.adamec2.util.TimeParser;

import java.util.Random;

public class TestSimulation extends EventCore {

    private final Arrival<TestArrivalEvent, Customer> arrival;
    private final ServiceFront<TestServiceFrontEvent, Customer> serviceFront;

    private final MeanStat averageTimeInSim;

    private double lastAvgTimeInSim = -1;
    private double lastAvgTimeInQueue = -1;
    private double lastAvgQueueLength = -1;

    private int currentReplication = 0;
    private int totalReplications = 0;

    public TestSimulation(double maxTime, Random seedGen) {
        super(maxTime);

        // ---- Arrival initialization
        Gen<Double> interarrivalGen = new ExponentialGen(60.0);
        arrival = new Arrival<>(interarrivalGen, new CustomerFactory(), this);
        TestArrivalEvent arrivalEvent = new TestArrivalEvent(0.0, this, null, arrival);
        arrival.addArrivalEvent(arrivalEvent);

        // ---- Front service initialization
        Gen<Double> serviceGen = new TriangularGen(0.5 / 60.0, 1.3 / 60.0, 0.7 / 60.0);
        Front<Customer> queue = new Front<>(this);
        serviceFront = new ServiceFront<>(queue, serviceGen, this);
        TestFactory factory = new TestFactory(serviceFront);
        serviceFront.setEventFactory(factory);

        averageTimeInSim = new MeanStat();
    }

    public void startSimulation(double maxTime, int numberOfReplications) {
        this.maxTime = maxTime;
        this.totalReplications = numberOfReplications;
        runSimulation(numberOfReplications);
    }

    @Override
    protected void beforeReplication(int replicationNumber) {
        super.beforeReplication(replicationNumber);
        this.currentReplication = replicationNumber;
        time = 0.0;
        arrival.planNextArrivalEvent();
    }

    @Override
    protected void afterReplication(int replicationNumber) {
        lastAvgTimeInSim = averageTimeInSim.getMean();
        lastAvgTimeInQueue = serviceFront.averageQueueWaitTime();
        lastAvgQueueLength = serviceFront.averageQueueLength();

        System.out.println("Average time in simulation: " + TimeParser.parseTime(lastAvgTimeInSim));
        System.out.println("Average time in queue:      " + TimeParser.parseTime(lastAvgTimeInQueue));
        System.out.println("Average length of queue:    " + lastAvgQueueLength);
    }

    @Override
    protected void afterSimulation(int numberOfCompletedReplications) {
        super.afterSimulation(numberOfCompletedReplications);
    }

    public void planArrivalToService(Customer customer) {
        serviceFront.planNewEntityArrival(customer);
    }

    public void addAverageTimeInSim(double timeInSim) {
        averageTimeInSim.add(timeInSim);
    }

    public int getCurrentReplication() {
        return currentReplication;
    }

    public int getTotalReplications() {
        return totalReplications;
    }

    public double getLastAvgTimeInSim() {
        return lastAvgTimeInSim;
    }

    public double getLastAvgTimeInQueue() {
        return lastAvgTimeInQueue;
    }

    public double getLastAvgQueueLength() {
        return lastAvgQueueLength;
    }
}
