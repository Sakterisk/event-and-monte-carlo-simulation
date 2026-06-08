package sk.uniza.adamec2.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.TestSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.util.TimeParser;

import java.util.List;
import java.util.Random;

/**
 * JavaFX GUI for sem_two EventSim simulation.
 * <p>
 * The simulation runs on a dedicated background thread. All UI updates
 * happen via the single {@link #refresh(EventCore)} method that the core
 * calls through the {@link SimulationObserver} callback.
 */
public class SimulationGUI2 extends Application {

    // ── simulation state ──────────────────────────────────────────────────────
    private TestSimulation sim;
    private Thread simThread;
    private int replications = 10;
    private String simHours = "8:00:00";

    // ── controls (written by JavaFX thread, read on both) ─────────────────────
    private TextField hoursField;
    private TextField replField;
    private Button startBtn;
    private Button stopBtn;
    private Button pauseBtn;
    private Slider speedSlider;
    private Label sliderLabel;

    // ── display labels ────────────────────────────────────────────────────────
    private Label simTimeLabel;
    private Label replicationLabel;
    private Label serverStatusLabel;
    private Label servedPersonLabel;
    private Label queueLengthLabel;
    private Label avgTimeSimLabel;
    private Label avgTimeQueueLabel;
    private Label avgQueueLenLabel;
    private ListView<String> queueListView;

    // ── speed slider values ───────────────────────────────────────────────────
    // leftmost=0  → sleepTime=250  (real-time, 1 sim-sec ≈ 1 real-sec)
    // middle=50   → sleepTime=125  (2×)
    // rightmost=100 → sleepTime=-1 (max speed, no sleep)
    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;
    private static final int SLIDER_START = 50;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void start(Stage stage) {
        stage.setTitle("EventSim – Airport Security (S2)");

        // create the core once; we reuse / recreate it for each run
        Random seedGen = new Random();
        sim = new TestSimulation(TimeParser.parseTime(simHours), seedGen);
        sim.subscribe(this::refresh);

        // ── layout ────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #1e1e2e;");

        root.setTop(buildControlPanel());
        root.setCenter(buildInfoPanel());

        Scene scene = new Scene(root, 720, 560);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> stopSimulation());
        stage.show();
    }

    // ── panel builders ────────────────────────────────────────────────────────

    private VBox buildControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 0, 12, 0));

        // ── inputs row ────────────────────────────────────────────────────────
        hoursField = new TextField(simHours);
        hoursField.setPrefWidth(70);
        replField = new TextField(String.valueOf(replications));
        replField.setPrefWidth(70);

        HBox inputs = labeledRow(
                "Hours:", hoursField,
                "Replications:", replField
        );

        // ── buttons row ───────────────────────────────────────────────────────
        startBtn = new Button("▶ Start");
        stopBtn = new Button("■ Stop");
        pauseBtn = new Button("⏸ Pause");

        stopBtn.setDisable(true);
        pauseBtn.setDisable(true);

        styleButton(startBtn, "#4caf50");
        styleButton(stopBtn, "#f44336");
        styleButton(pauseBtn, "#ff9800");

        startBtn.setOnAction(e -> startSimulation());
        stopBtn.setOnAction(e -> stopSimulation());
        pauseBtn.setOnAction(e -> togglePause());

        HBox buttons = new HBox(10, startBtn, stopBtn, pauseBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        // ── speed slider ──────────────────────────────────────────────────────
        speedSlider = new Slider(SLIDER_MIN, SLIDER_MAX, SLIDER_START);
        speedSlider.setPrefWidth(300);
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);

        sliderLabel = styledLabel(sliderDescription(SLIDER_START), "#90caf9", 12);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int mapped = mapSliderToSleep(newVal.intValue());
            sliderLabel.setText(sliderDescription(newVal.intValue()));
            if (sim != null) {
                sim.changeSleepTime(mapped);
            }
        });

        HBox sliderRow = new HBox(10,
                styledLabel("Real-time", "#aaa", 11),
                speedSlider,
                styledLabel("Max speed", "#aaa", 11),
                sliderLabel
        );
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(inputs, buttons, sliderRow);
        return panel;
    }

    private GridPane buildInfoPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: #2a2a3e; -fx-background-radius: 8;");

        // column constraints
        ColumnConstraints c1 = new ColumnConstraints(160);
        ColumnConstraints c2 = new ColumnConstraints(220);
        ColumnConstraints c3 = new ColumnConstraints(160);
        ColumnConstraints c4 = new ColumnConstraints(140);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);

        // ── sim state column ──────────────────────────────────────────────────
        simTimeLabel = valueLabel("0:00:00");
        replicationLabel = valueLabel("0 / 0");
        serverStatusLabel = valueLabel("Idle");
        servedPersonLabel = valueLabel("—");

        int row = 0;
        grid.add(sectionHeader("Simulation"), 0, row++, 2, 1);
        grid.add(captionLabel("Sim time:"), 0, row);
        grid.add(simTimeLabel, 1, row++);
        grid.add(captionLabel("Replication:"), 0, row);
        grid.add(replicationLabel, 1, row++);
        grid.add(captionLabel("Server:"), 0, row);
        grid.add(serverStatusLabel, 1, row++);
        grid.add(captionLabel("Serving:"), 0, row);
        grid.add(servedPersonLabel, 1, row++);

        // ── queue column ──────────────────────────────────────────────────────
        queueLengthLabel = valueLabel("0");
        queueListView = new ListView<>();
        queueListView.setPrefHeight(160);
        queueListView.setPrefWidth(200);
        queueListView.setStyle("-fx-control-inner-background: #1e1e2e; -fx-text-fill: #cdd6f4;");

        int qRow = 0;
        grid.add(sectionHeader("Queue"), 2, qRow++, 2, 1);
        grid.add(captionLabel("Length:"), 2, qRow);
        grid.add(queueLengthLabel, 3, qRow++);
        grid.add(captionLabel("Persons:"), 2, qRow++);
        grid.add(queueListView, 2, qRow++, 2, 4);

        // ── stats row at bottom ───────────────────────────────────────────────
        avgTimeSimLabel = valueLabel("—");
        avgTimeQueueLabel = valueLabel("—");
        avgQueueLenLabel = valueLabel("—");

        int sRow = row + 1;
        grid.add(sectionHeader("Stats (last replication)"), 0, sRow++, 4, 1);
        grid.add(captionLabel("Avg time in sim:"), 0, sRow);
        grid.add(avgTimeSimLabel, 1, sRow);
        grid.add(captionLabel("Avg time in queue:"), 2, sRow);
        grid.add(avgTimeQueueLabel, 3, sRow++);
        grid.add(captionLabel("Avg queue length:"), 0, sRow);
        grid.add(avgQueueLenLabel, 1, sRow);

        return grid;
    }

    // ── simulation lifecycle ──────────────────────────────────────────────────

    private void createNewSim() {

    }

    private void startSimulation() {
        if (simThread != null && simThread.isAlive()) return;

        double maxTime = 8.0;
        try {
            if (hoursField != null) {
                simHours = hoursField.getText();
            }
            maxTime = TimeParser.parseTime(simHours);
            replications = Integer.parseInt(replField != null ? replField.getText() : String.valueOf(replications));
        } catch (NumberFormatException ignored) {
        }

        sim.changeSleepTime(mapSliderToSleep((int) (speedSlider != null ? speedSlider.getValue() : SLIDER_START)));

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        pauseBtn.setDisable(false);
        pauseBtn.setText("⏸ Pause");

        final int reps = replications;
        final double mTime = maxTime;
        simThread = new Thread(() -> {
            sim.startSimulation(mTime, reps);
            Platform.runLater(this::onSimulationEnded);
        });
        simThread.setDaemon(true);
        simThread.start();
    }

    private void stopSimulation() {
        if (sim != null) {
            sim.setSimulationRunning(false);
            sim.setPaused(false);
        }
    }

    private void togglePause() {
        if (sim == null) return;
        boolean paused = !sim.isPaused();
        sim.setPaused(paused);
        pauseBtn.setText(paused ? "▶ Resume" : "⏸ Pause");
    }

    private void onSimulationEnded() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        pauseBtn.setDisable(true);
        pauseBtn.setText("⏸ Pause");
        serverStatusLabel.setText("Idle");
        serverStatusLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
    }

    // ── THE single refresh method called by the core ──────────────────────────

    /**
     * Called by the simulation core (on the sim thread) after each event.
     * All UI mutations are dispatched to the JavaFX Application Thread via
     * {@link Platform#runLater}.
     *
     * @param snapshotCore the current EventCore instance (same object, used as snapshot source)
     */
    public void refresh(EventCore snapshotCore) {
//        TestSimulation snapshot = (TestSimulation) snapshotCore;
//        // Capture all state BEFORE handing off to the FX thread
//        final double currentTime = snapshot.getTime();
//        final int currentRep = snapshot.getCurrentReplication();
//        final int totalReps = snapshot.getTotalReplications();
//        final boolean busy = !snapshot.isServiceAvailable();
//        final Person serving = snapshot.getPersonInService();
//        final int qLen = snapshot.getQueueSize();
//        final List<String> qIds = snapshot.getQueuePersonIds();
//        final double avgTimeSim = snapshot.getLastAvgTimeInSim();
//        final double avgTimeQueue = snapshot.getLastAvgTimeInQueue();
//        final double avgQueueLen = snapshot.getLastAvgQueueLength();
//
//        Platform.runLater(() -> {
//            simTimeLabel.setText(TimeParser.parseTime(currentTime));
//            replicationLabel.setText(currentRep + " / " + totalReps);
//
//            if (busy) {
//                serverStatusLabel.setText("Busy");
//                serverStatusLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold;");
//            } else {
//                serverStatusLabel.setText("Idle");
//                serverStatusLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
//            }
//
//            servedPersonLabel.setText(serving != null ? serving.toString() : "—");
//
//            queueLengthLabel.setText(String.valueOf(qLen));
//            queueListView.getItems().setAll(qIds);
//
//            if (avgTimeSim >= 0) {
//                avgTimeSimLabel.setText(TimeParser.parseTime(avgTimeSim));
//                avgTimeQueueLabel.setText(TimeParser.parseTime(avgTimeQueue));
//                avgQueueLenLabel.setText(String.format("%.3f", avgQueueLen));
//            }
//        });
    }

    // ── helper: slider ↔ sleepTime mapping ────────────────────────────────────

    /**
     * Maps slider value [0..100] to sleepTime:
     * 0   → 250 ms  (1× real-time)
     * 50  → 125 ms  (2×)
     * 100 → -1      (max speed)
     */
    private int mapSliderToSleep(int sliderVal) {
        if (sliderVal >= SLIDER_MAX) return -1;
        // linear interpolation: 0→250, 99→1
        return (int) Math.round(250.0 - (249.0 * sliderVal / (SLIDER_MAX - 1)));
    }

    private String sliderDescription(int sliderVal) {
        int sleep = mapSliderToSleep(sliderVal);
        if (sleep == -1) return "Max speed";
        if (sleep == 250) return "Real-time (1×)";
        return String.format("%.1f× speed", 250.0 / sleep);
    }

    // ── style helpers ─────────────────────────────────────────────────────────

    private HBox labeledRow(String l1, Control c1, String l2, Control c2) {
        HBox row = new HBox(10,
                styledLabel(l1, "#cdd6f4", 12), c1,
                styledLabel(l2, "#cdd6f4", 12), c2
        );
        row.setAlignment(Pos.CENTER_LEFT);
        styleInput(c1);
        styleInput(c2);
        return row;
    }

    private void styleInput(Control c) {
        c.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; -fx-border-color: #585b70; -fx-border-radius: 4;");
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 6;");
        btn.setPrefWidth(100);
    }

    private Label styledLabel(String text, String color, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + ";");
        return l;
    }

    private Label captionLabel(String text) {
        return styledLabel(text, "#a6adc8", 12);
    }

    private Label valueLabel(String text) {
        Label l = styledLabel(text, "#cdd6f4", 13);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        return l;
    }

    private Label sectionHeader(String text) {
        Label l = styledLabel(text, "#89b4fa", 13);
        l.setFont(Font.font(null, FontWeight.BOLD, 13));
        return l;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
