package sk.uniza.adamec2.sem_one;

import sk.uniza.adamec2.generator.ContinuousUniformGen;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.gui.SimulationListener;
import sk.uniza.adamec2.util.TimeParser;
import sk.uniza.adamec2.core.MonteCarloCore;
import sk.uniza.adamec2.sem_one.road.*;
import sk.uniza.adamec2.sem_one.segment.*;

import java.util.Random;

public class SemMonteCarlo extends MonteCarloCore {

    // 0 - Zilina(Z), 1 - Strecno(S), 2 - Rajecke Teplice(R), 3 - Divinka(D)

    private final Segment[][] segments;
    private final Road[] kRoads;
    private final Gen<Double> slowdownGen;
    private int[] order = new int[]{0, 1, 2, 3, 0};
    private double startTime = 6.0;
    private double slowdownStartTime = 6.5;
    private double time;
    private double timeSum;

    private double endUntilTime = 7.0 + 35.0 / 60.0;
    private boolean isU2 = false;
    private int counterEndUntilTime = 0;

    // GUI communication
    private SimulationListener listener;

    public SemMonteCarlo() {
        segments = new Segment[4][4];
        initializeSegments();
        kRoads = new Road[]{
                new BlackRoad(2),
                new BlueRoad(4),
                new GreenRoad(2),
                new RedRoad(2)
        };
        this.slowdownGen = new ContinuousUniformGen(10.0, 25.0);
    }

    private void initializeSegments() {
        ZSSegment zsSegment = new ZSSegment();
        ZDSegment zdSegment = new ZDSegment();
        DRSegment drSegment = new DRSegment();
        SRSegment srSegment = new SRSegment();
        segments[0][1] = zsSegment;
        segments[1][0] = zsSegment;
        segments[0][3] = zdSegment;
        segments[3][0] = zdSegment;
        segments[2][3] = drSegment;
        segments[3][2] = drSegment;
        segments[1][2] = srSegment;
        segments[2][1] = srSegment;
    }

    public void setListener(SimulationListener listener) {
        this.listener = listener;
    }

    public void startNewSimulation(int[] newOrder, int numberOfReplications, double startTime, double slowdownStartTime) {
        this.startTime = startTime;
        this.order = newOrder;
        this.slowdownStartTime = slowdownStartTime;
        this.isU2 = false;
        runSimulation(numberOfReplications);
    }

    public void startNewSimulation(int[] newOrder, int numberOfReplications, double startTime, double slowdownStartTime, double endUntilTime) {
        this.startTime = startTime;
        this.order = newOrder;
        this.slowdownStartTime = slowdownStartTime;
        this.endUntilTime = endUntilTime;
        this.isU2 = true;
        runSimulation(numberOfReplications);
    }

    @Override
    protected void beforeSimulation(int numberOfReplications) {
        timeSum = 0;
        counterEndUntilTime = 0;
    }

    @Override
    protected void beforeReplication(int replicationNumber) {
        time = startTime;
    }

    @Override
    protected void simulateReplication(int replicationNumber) {
        for (int i = 0; i < order.length - 1; i++) {
            time += getBestTimeToPass(order[i], order[i + 1]);
        }
    }

    @Override
    protected void afterReplication(int replicationNumber) {
        if (!isU2) {
            timeSum += time;
            double currentAverage = timeSum / replicationNumber;
            if (listener != null) {
                listener.onReplicationComplete(replicationNumber, currentAverage);
            }
        } else {
            if (time <= endUntilTime) {
                counterEndUntilTime++;
            }
            double currentPercent = (double) counterEndUntilTime / replicationNumber;
            if (listener != null) {
                listener.onReplicationComplete(replicationNumber, currentPercent);
            }
        }

    }

    @Override
    protected void afterSimulation(int numberOfCompletedReplications) {
        if (!isU2) {
            double finalAverage = timeSum / numberOfCompletedReplications;
            System.out.println("Average time: " + TimeParser.parseTime(finalAverage));

            if (listener != null) {
                if (shouldSimulationStop()) {
                    listener.onSimulationStopped(finalAverage, numberOfCompletedReplications);
                } else {
                    listener.onSimulationComplete(finalAverage);
                }
            }
        } else {
            double finalPercent = (double) counterEndUntilTime / numberOfCompletedReplications;
            System.out.println("Percent of replications with time > " + TimeParser.parseTime(endUntilTime) + ": " + String.format("%.2f", finalPercent * 100) + "%");

            if (listener != null) {
                if (shouldSimulationStop()) {
                    listener.onSimulationStopped(finalPercent, numberOfCompletedReplications);
                } else {
                    listener.onSimulationComplete(finalPercent);
                }
            }
        }
    }

    private double getBestTimeToPass(int from, int to) {
        double timeToPassKRoads = getTimeToPassKRoads(from, to);
        if (segments[from][to] == null) {
            return timeToPassKRoads;
        }
        double timeToPassSegment = segments[from][to].getBestTimeToPass();
        return Math.min(timeToPassKRoads, timeToPassSegment);
    }

    private double getTimeToPassKRoads(int from, int to) {
        double firstRoadTime = kRoads[from].getTimeToPass();
        double secondRoadTime = 0;
        if (time + firstRoadTime > slowdownStartTime) {
            double slowdown = slowdownGen.next();
            secondRoadTime = kRoads[to].getSlowdownTimeToPass(slowdown);
        } else {
            secondRoadTime = kRoads[to].getTimeToPass();
        }
        return firstRoadTime + secondRoadTime;
    }
}
