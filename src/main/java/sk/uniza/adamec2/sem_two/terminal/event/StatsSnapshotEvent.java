package sk.uniza.adamec2.sem_two.terminal.event;

import sk.uniza.adamec2.core.Event;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * StatsSnapshotEvent – fires every INTERVAL simulation-minutes and appends
 * one CSV row with the current within-replication statistics.
 *
 * CSV columns: time, avgTimeInSim, avgQueueBP (before-place), avgQueueBT (before-take)
 *
 * Schedule the first instance in TerminalSimulation.beforeReplication() right
 * after resetStatistics() is called, e.g.:
 *
 *   addEvent(new StatsSnapshotEvent(WARM_UP + INTERVAL, this, writer));
 *
 * The event re-schedules itself automatically until maxTime is reached.
 */
public class StatsSnapshotEvent extends Event {

    /** How often (in simulation hours) a snapshot is taken – 1 minute = 1/60 h */
    public static final double INTERVAL = 1.0 / 60.0;

    private final TerminalSimulation sim;
    private final PrintWriter writer;

    /**
     * @param time   scheduled simulation time for this snapshot
     * @param sim    the running TerminalSimulation (gives us the live stats)
     * @param writer CSV writer shared across all snapshots in one replication
     */
    public StatsSnapshotEvent(double time, TerminalSimulation sim, PrintWriter writer) {
        super(time, sim);
        this.sim    = sim;
        this.writer = writer;
    }

    @Override
    public void execute() {
        // --- read live within-replication stats ---
        double avgTimeInSim = sim.getAverageTimeInSimSnapshot();   // add this getter – see notes
        double avgQueueBP   = sim.getAverageQueueLengthBeforePlaceCrateSnapshot();
        double avgQueueBT   = sim.getAverageQueueLengthBeforeTakeCrateSnapshot();

        writer.printf("%.6f,%.6f,%.6f,%.6f%n",
                getTime(), avgTimeInSim, avgQueueBP, avgQueueBT);
        writer.flush();

        // --- re-schedule for next minute ---
        double nextTime = getTime() + INTERVAL;
        if (nextTime <= sim.getMaxTime()) {
            sim.addEvent(new StatsSnapshotEvent(nextTime, sim, writer));
        }
    }

    // -------------------------------------------------------------------------
    // Helper: open a fresh CSV file for one replication.
    // Call this in TerminalSimulation.beforeReplication() and keep the writer.
    // -------------------------------------------------------------------------
    public static PrintWriter openCsvWriter(int replication) throws IOException {
        String filename = "warmup_stats_rep_" + replication + ".csv";
        PrintWriter pw = new PrintWriter(new FileWriter(filename, false));
        pw.println("time,avgTimeInSim,avgQueueBP,avgQueueBT");
        return pw;
    }
}