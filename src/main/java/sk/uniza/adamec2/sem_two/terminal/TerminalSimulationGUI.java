package sk.uniza.adamec2.sem_two.terminal;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.FrontableEntity;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.segment.SingleTerminal;
import sk.uniza.adamec2.stat.MeanStat;
import sk.uniza.adamec2.stat.TimeWeightedMeanStat;
import sk.uniza.adamec2.util.TimeParser;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-mode JavaFX GUI for sem_two terminal simulation.
 *
 * Modes:
 *  - Interactive: live simulation with detailed per-terminal queues and services.
 *  - Capacity sweep: run multiple capacities and show averages in a table + CSV export.
 *  - Capacity range: run around a center capacity and plot a line chart.
 */
public class TerminalSimulationGUI extends Application {

    // ---- dark theme palette ----
    private static final String COLOR_BG = "#1e1e2e";
    private static final String COLOR_PANEL = "#242438";
    private static final String COLOR_PANEL_DARK = "#1b1b2b";
    private static final String COLOR_INPUT = "#313244";
    private static final String COLOR_HEADER = "#3b3f5c";
    private static final String COLOR_SELECTION = "#45475a";
    private static final String COLOR_TEXT = "#cdd6f4";
    private static final String COLOR_TEXT_DIM = "#a6adc8";
    private static final String COLOR_ACCENT = "#89b4fa";
    private static final String COLOR_SUCCESS = "#a6e3a1";
    private static final String COLOR_ERROR = "#f38ba8";
    private static final String COLOR_BORDER = "#585b70";

    // =====================================================================
    // Simulation state (interactive mode)
    // =====================================================================

    private TerminalSimulation sim;
    private Thread simThread;
    private int replications = 10;
    private int capacityPerDay = 500;

    // speed slider
    private Slider speedSlider;
    private Label sliderLabel;
    private CheckBox fullSpeedCheckBox;
    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 249;
    private static final int SLIDER_START = 0;

    // controls (interactive)
    private TextField replField;
    private TextField capacityField;
    private Button startBtn;
    private Button stopBtn;
    private Button pauseBtn;

    // simulation info labels
    private Label simTimeLabel;
    private Label replicationLabel;
    private Label nextArrivalEntityLabel;
    private Label nextArrivalTimeLabel;
    private Label avgTimeSimLabel;
    private Label avgQueuePlaceLabel;
    private Label avgQueueTakeLabel;

    // terminal views (interactive)
    private TerminalView t1View;
    private TerminalView t2View;

    // capacities – interactive
    private TextField beforeXrayField;
    private TextField afterXrayField;

    // capacities – sweep
    private TextField sweepBeforeXrayField;
    private TextField sweepAfterXrayField;

    // capacities – range
    private TextField rangeBeforeXrayField;
    private TextField rangeAfterXrayField;

    // capacities – replication graph
    private TextField repGraphBeforeXrayField;
    private TextField repGraphAfterXrayField;

    // =====================================================================
    // Mode 2: capacity sweep
    // =====================================================================

    private TextField sweepReplField;
    private TextField sweepStartCapField;
    private TextField sweepEndCapField;
    private TextField sweepStepField;
    private CheckBox sweepCustomStepCheckBox;
    private Button sweepStartBtn;
    private Button sweepStopBtn;
    private Thread sweepThread;
    private volatile boolean sweepRunning = false;

    private final javafx.collections.ObservableList<SweepRow> sweepData =
            javafx.collections.FXCollections.observableArrayList();

    public record SweepRow(int capacity, String avgTimeInSim, double avgQueuePlace, double avgQueueTake) {
    }

    // =====================================================================
    // Mode 3: capacity range graph
    // =====================================================================

    private TextField rangeReplField;
    private TextField rangeCapField;
    private TextField rangePercentField;
    private TextField rangePointsField;
    private Button rangeStartBtn;
    private Button rangeStopBtn;
    private Thread rangeThread;
    private volatile boolean rangeRunning = false;

    // Mode 4: replication vs time graph
    private TextField repGraphReplField;
    private TextField repGraphCapacityField;
    private Button repGraphStartBtn;
    private Button repGraphStopBtn;

    private LineChart<Number, Number> repGraphChart;
    private XYChart.Series<Number, Number> repGraphMeanSeries;
    private XYChart.Series<Number, Number> repGraphUpperSeries;
    private XYChart.Series<Number, Number> repGraphLowerSeries;

    private Thread repGraphThread;
    private volatile boolean repGraphRunning = false;

    // =====================================================================
    // Snapshot records (used by interactive mode)
    // =====================================================================

    public static record FrontSnapshot(
            String name,
            int size,
            List<String> entities,
            double avgWaitTime,
            double avgLength
    ) { }

    public static record ServiceSnapshot(
            String name,
            boolean busy,
            String currentEntity,
            double endServiceTime,
            FrontSnapshot frontSnapshot
    ) { }

    public static record ArrivalSnapshot(
            String name,
            String nextEntity,
            double nextArrivalTime
    ) { }

    private static record TerminalViewSnapshots(
            FrontSnapshot placeFront,
            ServiceSnapshot xrayService,
            ServiceSnapshot detectorService,
            FrontSnapshot afterXrayFront,
            FrontSnapshot beforeTakeFront
    ) { }

    // =====================================================================
    // JavaFX lifecycle
    // =====================================================================

    @Override
    public void start(Stage stage) {
        stage.setTitle("EventSim – Airport Terminal (S2)");

        sim = new TerminalSimulation();
        sim.subscribe(this::refresh);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-tab-min-width: 120;" +
                        "-fx-tab-max-width: 200;" +
                        "-fx-tab-min-height: 36;"
        );

        Tab interactiveTab = new Tab("Interactive");
        interactiveTab.setContent(buildInteractivePane());

        Tab sweepTab = new Tab("Capacity sweep");
        sweepTab.setContent(buildCapacitySweepPane());

        Tab rangeTab = new Tab("Capacity range");
        rangeTab.setContent(buildCapacityRangePane());

        Tab repGraphTab = new Tab("Replications graph");
        repGraphTab.setContent(buildReplicationGraphPane());

        tabs.getTabs().addAll(interactiveTab, sweepTab, rangeTab, repGraphTab);

        root.setCenter(tabs);

        Scene scene = new Scene(root, 1100, 650);
        applyGlobalDarkTheme(scene, tabs);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> stopSimulation());
        stage.show();
    }

    // =====================================================================
    // Interactive mode UI
    // =====================================================================

    private Node buildInteractivePane() {
        Node controlPanel = buildControlPanel();
        GridPane infoGrid = buildInfoGrid();

        ScrollPane scroll = new ScrollPane(infoGrid);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        styleScrollPane(scroll);

        VBox root = new VBox(10, controlPanel, scroll);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");
        return root;
    }

    private Node buildControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(0, 0, 12, 0));

        replField = new TextField(String.valueOf(replications));
        capacityField = new TextField(String.valueOf(capacityPerDay));
        replField.setPrefWidth(80);
        capacityField.setPrefWidth(80);
        styleInput(replField);
        styleInput(capacityField);

        // NEW: capacities before/after X-ray
        beforeXrayField = new TextField("4");
        afterXrayField = new TextField("5");
        beforeXrayField.setPrefWidth(60);
        afterXrayField.setPrefWidth(60);
        styleInput(beforeXrayField);
        styleInput(afterXrayField);

        HBox inputs = new HBox(10,
                captionLabel("Replications:"), replField,
                captionLabel("Capacity/day:"), capacityField,
                captionLabel("Before X-ray cap:"), beforeXrayField,
                captionLabel("After X-ray cap:"), afterXrayField
        );
        inputs.setAlignment(Pos.CENTER_LEFT);

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

        // speed slider + full-speed checkbox
        speedSlider = new Slider(SLIDER_MIN, SLIDER_MAX, SLIDER_START);
        speedSlider.setPrefWidth(300);
        speedSlider.setMajorTickUnit(25);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);

        sliderLabel = styledLabel(sliderDescription(SLIDER_START), "#90caf9", 12);

        fullSpeedCheckBox = new CheckBox("Full speed");
        styleCheckBox(fullSpeedCheckBox);

        fullSpeedCheckBox.selectedProperty().addListener((obs, oldVal, selected) -> {
            if (selected) {
                speedSlider.setDisable(true);
                sliderLabel.setText("Max speed");
                if (sim != null) {
                    sim.changeSleepTime(-1);
                }
            } else {
                speedSlider.setDisable(false);
                int sliderVal = (int) speedSlider.getValue();
                sliderLabel.setText(sliderDescription(sliderVal));
                int mapped = mapSliderToSleep(sliderVal);
                if (sim != null) {
                    sim.changeSleepTime(mapped);
                }
            }
        });

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (fullSpeedCheckBox != null && fullSpeedCheckBox.isSelected()) {
                return;
            }
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
                sliderLabel,
                fullSpeedCheckBox
        );
        sliderRow.setAlignment(Pos.CENTER_LEFT);

        panel.getChildren().addAll(inputs, buttons, sliderRow);
        return panel;
    }

    private GridPane buildInfoGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: " + COLOR_PANEL + "; -fx-background-radius: 8;");

        ColumnConstraints c1 = new ColumnConstraints(260);
        ColumnConstraints c2 = new ColumnConstraints(260);
        ColumnConstraints c3 = new ColumnConstraints(260);
        ColumnConstraints c4 = new ColumnConstraints(260);
        grid.getColumnConstraints().addAll(c1, c2, c3, c4);

        int row = 0;

        // Simulation info
        grid.add(sectionHeader("Simulation"), 0, row++, 4, 1);

        simTimeLabel = valueLabel("0:00:00");
        replicationLabel = valueLabel("0 / 0");
        nextArrivalEntityLabel = valueLabel("—");
        nextArrivalTimeLabel = valueLabel("—");
        avgTimeSimLabel = valueLabel("—");
        avgQueuePlaceLabel = valueLabel("—");
        avgQueueTakeLabel = valueLabel("—");

        grid.add(captionLabel("Sim time:"), 0, row);
        grid.add(simTimeLabel, 1, row++);
        grid.add(captionLabel("Replication:"), 0, row);
        grid.add(replicationLabel, 1, row++);

        grid.add(captionLabel("Next arrival entity:"), 0, row);
        grid.add(nextArrivalEntityLabel, 1, row++);
        grid.add(captionLabel("Next arrival time:"), 0, row);
        grid.add(nextArrivalTimeLabel, 1, row++);

        grid.add(captionLabel("Avg time in system:"), 0, row);
        grid.add(avgTimeSimLabel, 1, row++);
        grid.add(captionLabel("Avg queue len before place:"), 0, row);
        grid.add(avgQueuePlaceLabel, 1, row++);
        grid.add(captionLabel("Avg queue len before take:"), 0, row);
        grid.add(avgQueueTakeLabel, 1, row++);

        // Terminal views
        t1View = new TerminalView("Terminal 1");
        t2View = new TerminalView("Terminal 2");

        GridPane t1Pane = t1View.buildPane();
        GridPane t2Pane = t2View.buildPane();

        grid.add(t1Pane, 0, row, 2, 1);
        grid.add(t2Pane, 2, row, 2, 1);

        return grid;
    }

    // =====================================================================
    // Simulation lifecycle (interactive)
    // =====================================================================

    private void startSimulation() {
        if (simThread != null && simThread.isAlive()) {
            return;
        }

        try {
            replications = Integer.parseInt(replField.getText());
            capacityPerDay = Integer.parseInt(capacityField.getText());
        } catch (NumberFormatException ignored) {
        }

        int[] caps = parseFrontCapacities(beforeXrayField, afterXrayField);
        int beforeCap = caps[0];
        int afterCap = caps[1];

        int initialSleep;
        if (fullSpeedCheckBox != null && fullSpeedCheckBox.isSelected()) {
            initialSleep = -1;
        } else {
            int sliderVal = (int) (speedSlider != null ? speedSlider.getValue() : SLIDER_START);
            initialSleep = mapSliderToSleep(sliderVal);
        }
        sim.changeSleepTime(initialSleep);

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        pauseBtn.setDisable(false);
        pauseBtn.setText("⏸ Pause");

        final int reps = replications;
        final int cap = capacityPerDay;

        simThread = new Thread(() -> {
            sim.startSimulation(reps, cap, beforeCap, afterCap);
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
        if (sim == null) {
            return;
        }
        boolean paused = !sim.isPaused();
        sim.setPaused(paused);
        pauseBtn.setText(paused ? "▶ Resume" : "⏸ Pause");
    }

    private void onSimulationEnded() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        pauseBtn.setDisable(true);
        pauseBtn.setText("⏸ Pause");
    }

    // =====================================================================
    // Observer callback – build snapshots and update interactive view
    // =====================================================================

    public void refresh(EventCore snapshotCore) {
        if (!(snapshotCore instanceof TerminalSimulation)) {
            return;
        }
        TerminalSimulation snapshot = (TerminalSimulation) snapshotCore;

        final double currentTime = snapshot.getTime();
        final int currentRep = snapshot.getCurrentReplication();
        final int totalReps = snapshot.getTotalReplications();

        Person nextPerson = snapshot.getNextArrivalPerson();
        final ArrivalSnapshot arrivalSnap = new ArrivalSnapshot(
                "Arrivals",
                nextPerson != null ? "P" + nextPerson.getId() : "—",
                snapshot.getNextArrivalTime()
        );

        final double combAvgTimeSim = snapshot.getCombinedAverageTimeInSim();
        final double combAvgTimeSimVariance = snapshot.getCombinedAverageTimeInSimVariance();
        final double combAvgQueuePlace = snapshot.getCombinedAverageQueueLengthBeforePlaceCrate();
        final double combAvgQueuePlaceVariance = snapshot.getCombinedAverageQueueLengthBeforePlaceCrateVariance();
        final double combAvgQueueTake = snapshot.getCombinedAverageQueueLengthBeforeTakeCrate();
        final double combAvgQueueTakeVariance = snapshot.getCombinedAverageQueueLengthBeforeTakeCrateVariance();

        SingleTerminal t1 = snapshot.getTerminal1();
        SingleTerminal t2 = snapshot.getTerminal2();

        TerminalViewSnapshots t1Snaps = buildSnapshotsForTerminal("T1", t1);
        TerminalViewSnapshots t2Snaps = buildSnapshotsForTerminal("T2", t2);

        Platform.runLater(() -> {
            simTimeLabel.setText(TimeParser.parseTime(currentTime));
            replicationLabel.setText(currentRep + " / " + totalReps);

            nextArrivalEntityLabel.setText(arrivalSnap.nextEntity());
            nextArrivalTimeLabel.setText(TimeParser.parseTime(arrivalSnap.nextArrivalTime()));

            if (combAvgTimeSim > 0.0) {
                if (combAvgTimeSimVariance != -1.0) {
                    avgTimeSimLabel.setText(TimeParser.parseTime(combAvgTimeSim) + " ("
                            + TimeParser.parseTime(combAvgTimeSim - combAvgTimeSimVariance) + ";"
                            + TimeParser.parseTime(combAvgTimeSim + combAvgTimeSimVariance) +")"
                    );
                } else {
                    avgTimeSimLabel.setText(TimeParser.parseTime(combAvgTimeSim));
                }
            }
            if (combAvgQueuePlace > 0.0) {
                if (combAvgQueuePlaceVariance != -1.0) {
                    avgQueuePlaceLabel.setText(String.format("%.3f", combAvgQueuePlace) + " ("
                            + String.format("%.3f", combAvgQueuePlace - combAvgQueuePlaceVariance) + ";"
                            + String.format("%.3f", combAvgQueuePlace + combAvgQueuePlaceVariance) +")"
                    );
                } else {
                    avgQueuePlaceLabel.setText(String.format("%.3f", combAvgQueuePlace));
                }
            }
            if (combAvgQueueTake > 0.0) {
                if (combAvgQueueTakeVariance != -1.0) {
                    avgQueueTakeLabel.setText(String.format("%.3f", combAvgQueueTake) + " ("
                            + String.format("%.3f", combAvgQueueTake - combAvgQueueTakeVariance) + ";"
                            + String.format("%.3f", combAvgQueueTake + combAvgQueueTakeVariance) +")"
                    );
                } else {
                    avgQueueTakeLabel.setText(String.format("%.3f", combAvgQueueTake));
                }
            }

            t1View.updateFromSnapshots(t1Snaps);
            t2View.updateFromSnapshots(t2Snaps);
        });
    }

    private TerminalViewSnapshots buildSnapshotsForTerminal(String prefix, SingleTerminal t) {
        // place crate front
        Front<Person> placeFront = t.getFrontBeforePlaceCrateSegment();
        FrontSnapshot placeSnap = buildFrontSnapshot(
                prefix + " place crate front",
                placeFront,
                p -> {
                    // assuming Person has asLabelWithBaggage(); if not, use "P" + p.getId()
                    try {
                        return p.asLabelWithBaggage();
                    } catch (NoSuchMethodError e) {
                        return "P" + p.getId();
                    }
                }
        );

        // before take crate front
        Front<Person> beforeTakeFront = t.getFrontBeforeTakeCrateSegment();
        FrontSnapshot beforeTakeSnap = buildFrontSnapshot(
                prefix + " before take crate front",
                beforeTakeFront,
                p -> {
                    try {
                        return p.asLabelWithBaggage();
                    } catch (NoSuchMethodError e) {
                        return "P" + p.getId();
                    }
                }
        );

        // after X-ray front
        Front<Crate> afterXrayFront = t.getFrontAfterXraySegment();
        FrontSnapshot afterXraySnap = buildFrontSnapshot(
                prefix + " after X-ray front",
                afterXrayFront,
                c -> "C" + c.getId()
        );

        // X-ray service & front
        ServiceFront<?, Crate> xrayService = t.getXrayServiceSegment();
        Front<Crate> xrayFront = xrayService.getFront();
        FrontSnapshot xrayFrontSnap = buildFrontSnapshot(
                prefix + " X-ray front",
                xrayFront,
                c -> "C" + c.getId()
        );
        String xrayCurrent = xrayService.getEntity() != null
                ? "C" + xrayService.getEntity().getId()
                : "—";
        ServiceSnapshot xraySnap = new ServiceSnapshot(
                prefix + " X-ray service",
                !xrayService.isAvailable(),
                xrayCurrent,
                xrayService.getEndServiceTime(),
                xrayFrontSnap
        );

        // detector service & front
        ServiceFront<?, Person> detectorService = t.getDetectorServiceSegment();
        Front<Person> detectorFront = detectorService.getFront();
        FrontSnapshot detectorFrontSnap = buildFrontSnapshot(
                prefix + " detector front",
                detectorFront,
                p -> {
                    try {
                        return p.asLabelWithBaggage();
                    } catch (NoSuchMethodError e) {
                        return "P" + p.getId();
                    }
                }
        );
        String detectorCurrent = detectorService.getEntity() != null
                ? "P" + detectorService.getEntity().getId()
                : "—";
        ServiceSnapshot detectorSnap = new ServiceSnapshot(
                prefix + " detector service",
                !detectorService.isAvailable(),
                detectorCurrent,
                detectorService.getEndServiceTime(),
                detectorFrontSnap
        );

        return new TerminalViewSnapshots(
                placeSnap,
                xraySnap,
                detectorSnap,
                afterXraySnap,
                beforeTakeSnap
        );
    }

    private <E extends FrontableEntity> FrontSnapshot buildFrontSnapshot(
            String name,
            Front<E> front,
            java.util.function.Function<E, String> toStringFn
    ) {
        List<String> entities = front.getAll()
                .stream()
                .map(toStringFn)
                .collect(Collectors.toList());
        return new FrontSnapshot(
                name,
                front.size(),
                entities,
                front.averageTime(),
                front.averageLength()
        );
    }

    // =====================================================================
    // Mode 2: capacity sweep UI + logic
    // =====================================================================

    private Node buildCapacitySweepPane() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        sweepReplField = new TextField("100");
        sweepStartCapField = new TextField("100");
        sweepEndCapField = new TextField("200");
        sweepStepField = new TextField("1");
        sweepStepField.setDisable(true);

        styleInput(sweepReplField);
        styleInput(sweepStartCapField);
        styleInput(sweepEndCapField);
        styleInput(sweepStepField);

        sweepCustomStepCheckBox = new CheckBox("Custom step");
        styleCheckBox(sweepCustomStepCheckBox);

        // NEW: capacities for sweep experiments
        sweepBeforeXrayField = new TextField("4");
        sweepAfterXrayField = new TextField("5");
        sweepBeforeXrayField.setPrefWidth(60);
        sweepAfterXrayField.setPrefWidth(60);
        styleInput(sweepBeforeXrayField);
        styleInput(sweepAfterXrayField);

        sweepCustomStepCheckBox.selectedProperty().addListener((obs, o, selected) ->
                sweepStepField.setDisable(!selected)
        );

        HBox row1 = new HBox(10,
                captionLabel("Replications:"), sweepReplField,
                captionLabel("Start capacity:"), sweepStartCapField,
                captionLabel("End capacity:"), sweepEndCapField,
                captionLabel("Before X-ray cap:"), sweepBeforeXrayField,
                captionLabel("After X-ray cap:"), sweepAfterXrayField
        );
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox row2 = new HBox(10,
                sweepCustomStepCheckBox,
                captionLabel("Step:"), sweepStepField
        );
        row2.setAlignment(Pos.CENTER_LEFT);

        sweepStartBtn = new Button("Start experiment");
        sweepStopBtn = new Button("Stop experiment");
        sweepStopBtn.setDisable(true);
        styleButton(sweepStartBtn, "#4caf50");
        styleButton(sweepStopBtn, "#f44336");

        Button exportCsvBtn = new Button("Export CSV");
        styleButton(exportCsvBtn, "#2196f3");

        sweepStartBtn.setOnAction(e -> startCapacitySweep());
        sweepStopBtn.setOnAction(e -> stopCapacitySweep());
        exportCsvBtn.setOnAction(e -> exportSweepToCsv());

        HBox btnRow = new HBox(10, sweepStartBtn, sweepStopBtn, exportCsvBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        TableView<SweepRow> table = new TableView<>(sweepData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        styleTable(table);

        TableColumn<SweepRow, Number> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().capacity()));

        TableColumn<SweepRow, String> timeCol = new TableColumn<>("Avg time in sim");
        timeCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().avgTimeInSim()));

        TableColumn<SweepRow, Number> qPlaceCol = new TableColumn<>("Avg queue before place");
        qPlaceCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().avgQueuePlace()));

        TableColumn<SweepRow, Number> qTakeCol = new TableColumn<>("Avg queue before take");
        qTakeCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().avgQueueTake()));

        table.getColumns().addAll(capCol, timeCol, qPlaceCol, qTakeCol);

        root.getChildren().addAll(row1, row2, btnRow, table);
        return root;
    }

    private void startCapacitySweep() {
        if (sweepThread != null && sweepThread.isAlive()) return;

        int reps;
        int startCap;
        int endCap;
        int step;

        try {
            reps = Integer.parseInt(sweepReplField.getText());
            startCap = Integer.parseInt(sweepStartCapField.getText());
            endCap = Integer.parseInt(sweepEndCapField.getText());
            if (sweepCustomStepCheckBox.isSelected()) {
                step = Integer.parseInt(sweepStepField.getText());
            } else {
                step = 1;
            }
        } catch (NumberFormatException ex) {
            return;
        }
        if (step <= 0 || endCap < startCap) return;

        sweepData.clear();
        sweepRunning = true;
        sweepStartBtn.setDisable(true);
        sweepStopBtn.setDisable(false);

        sweepThread = new Thread(() -> {
            Random seedGen = new Random();

            int[] caps = parseFrontCapacities(sweepBeforeXrayField, sweepAfterXrayField);
            int beforeCap = caps[0];
            int afterCap = caps[1];
            for (int cap = startCap; cap <= endCap && sweepRunning; cap += step) {
                TerminalSimulation expSim = new TerminalSimulation();
                expSim.changeSleepTime(-1);
                expSim.startSimulation(reps, cap, beforeCap, afterCap);
                String avgTime = TimeParser.parseTime(expSim.getCombinedAverageTimeInSim());
                double avgQPlace = expSim.getCombinedAverageQueueLengthBeforePlaceCrate();
                double avgQTake = expSim.getCombinedAverageQueueLengthBeforeTakeCrate();

                SweepRow row = new SweepRow(cap, avgTime, avgQPlace, avgQTake);
                Platform.runLater(() -> sweepData.add(row));
                if (!sweepRunning) {
                    break;
                }
            }
            Platform.runLater(() -> {
                sweepRunning = false;
                sweepStartBtn.setDisable(false);
                sweepStopBtn.setDisable(true);
            });
        });
        sweepThread.setDaemon(true);
        sweepThread.start();
    }

    private void stopCapacitySweep() {
        sweepRunning = false;
    }

    private void exportSweepToCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save capacity sweep results");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var file = chooser.showSaveDialog(sweepStartBtn.getScene().getWindow());
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("capacity,avg_time_in_sim,avg_queue_before_place,avg_queue_before_take\n");
            for (SweepRow row : sweepData) {
                fw.write(row.capacity() + "," +
                        row.avgTimeInSim() + "," +
                        row.avgQueuePlace() + "," +
                        row.avgQueueTake() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================================
    // Mode 3: capacity range UI + logic
    // =====================================================================

    private Node buildCapacityRangePane() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        rangeReplField = new TextField("100");
        rangeCapField = new TextField("150");
        rangePercentField = new TextField("20");
        rangePointsField = new TextField("9");

        styleInput(rangeReplField);
        styleInput(rangeCapField);
        styleInput(rangePercentField);
        styleInput(rangePointsField);

        rangeBeforeXrayField = new TextField("4");
        rangeAfterXrayField = new TextField("5");
        rangeBeforeXrayField.setPrefWidth(60);
        rangeAfterXrayField.setPrefWidth(60);
        styleInput(rangeBeforeXrayField);
        styleInput(rangeAfterXrayField);

        HBox row1 = new HBox(10,
                captionLabel("Replications:"), rangeReplField,
                captionLabel("Capacity:"), rangeCapField,
                captionLabel("Percent ±:"), rangePercentField,
                captionLabel("Points:"), rangePointsField,
                captionLabel("Before X-ray cap:"), rangeBeforeXrayField,
                captionLabel("After X-ray cap:"), rangeAfterXrayField
        );
        row1.setAlignment(Pos.CENTER_LEFT);

        rangeStartBtn = new Button("Start experiment");
        rangeStopBtn = new Button("Stop experiment");
        rangeStopBtn.setDisable(true);
        styleButton(rangeStartBtn, "#4caf50");
        styleButton(rangeStopBtn, "#f44336");

        HBox btnRow = new HBox(10, rangeStartBtn, rangeStopBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Capacity");
        xAxis.setForceZeroInRange(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Time(s)");
        yAxis.setForceZeroInRange(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        styleChart(chart, xAxis, yAxis);

        XYChart.Series<Number, Number> seriesTime = new XYChart.Series<>();

        chart.getData().addAll(seriesTime);

        rangeStartBtn.setOnAction(e -> startCapacityRangeExperiment(
                chart, seriesTime));
        rangeStopBtn.setOnAction(e -> stopCapacityRangeExperiment());

        root.getChildren().addAll(row1, btnRow, chart);
        return root;
    }

    private void startCapacityRangeExperiment(LineChart<Number, Number> chart,
                                              XYChart.Series<Number, Number> seriesTime) {
        if (rangeThread != null && rangeThread.isAlive()) return;

        int reps;
        int centerCap;
        double percent;
        int points;

        try {
            reps = Integer.parseInt(rangeReplField.getText());
            centerCap = Integer.parseInt(rangeCapField.getText());
            percent = Double.parseDouble(rangePercentField.getText());
            points = Integer.parseInt(rangePointsField.getText());
        } catch (NumberFormatException ex) {
            return;
        }
        if (points < 2 || percent < 0) return;

        double factor = percent / 100.0;
        double low = centerCap * (1.0 - factor);
        double high = centerCap * (1.0 + factor);

        Set<Integer> capsSet = new TreeSet<>();
        if (points == 1) {
            capsSet.add(centerCap);
        } else {
            double step = (high - low) / (points - 1);
            for (int i = 0; i < points; i++) {
                int c = (int) Math.round(low + i * step);
                capsSet.add(c);
            }
            capsSet.add(centerCap);
        }
        List<Integer> capacities = new ArrayList<>(capsSet);

        seriesTime.getData().clear();

        rangeRunning = true;
        rangeStartBtn.setDisable(true);
        rangeStopBtn.setDisable(false);

        rangeThread = new Thread(() -> {
            Random seedGen = new Random();
            int[] caps = parseFrontCapacities(rangeBeforeXrayField, rangeAfterXrayField);
            int beforeCap = caps[0];
            int afterCap = caps[1];

            for (int cap : capacities) {
                if (!rangeRunning) break;
                TerminalSimulation expSim = new TerminalSimulation();
                expSim.changeSleepTime(-1);
                expSim.startSimulation(reps, cap, beforeCap, afterCap);
                double avgTime = expSim.getCombinedAverageTimeInSim();

                Platform.runLater(() -> {
                    seriesTime.getData().add(new XYChart.Data<>(cap, avgTime * 3600));
                });
            }
            Platform.runLater(() -> {
                rangeRunning = false;
                rangeStartBtn.setDisable(false);
                rangeStopBtn.setDisable(true);
            });
        });
        rangeThread.setDaemon(true);
        rangeThread.start();
    }

    private void stopCapacityRangeExperiment() {
        rangeRunning = false;
    }

    // 4.

    private Node buildReplicationGraphPane() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(8));
        root.setStyle("-fx-background-color: " + COLOR_BG + ";");

        // Inputs
        repGraphReplField = new TextField("1000");
        repGraphCapacityField = new TextField("500");

        styleInput(repGraphReplField);
        styleInput(repGraphCapacityField);

        repGraphBeforeXrayField = new TextField("4");
        repGraphAfterXrayField = new TextField("5");
        repGraphBeforeXrayField.setPrefWidth(60);
        repGraphAfterXrayField.setPrefWidth(60);
        styleInput(repGraphBeforeXrayField);
        styleInput(repGraphAfterXrayField);

        HBox inputs = new HBox(10,
                captionLabel("Replications:"), repGraphReplField,
                captionLabel("Capacity/day:"), repGraphCapacityField,
                captionLabel("Before X-ray cap:"), repGraphBeforeXrayField,
                captionLabel("After X-ray cap:"), repGraphAfterXrayField
        );
        inputs.setAlignment(Pos.CENTER_LEFT);

        // Buttons
        repGraphStartBtn = new Button("Start experiment");
        repGraphStopBtn = new Button("Stop experiment");
        repGraphStopBtn.setDisable(true);
        styleButton(repGraphStartBtn, "#4caf50");
        styleButton(repGraphStopBtn, "#f44336");

        repGraphStartBtn.setOnAction(e -> startReplicationGraphExperiment());
        repGraphStopBtn.setOnAction(e -> stopReplicationGraphExperiment());

        HBox btnRow = new HBox(10, repGraphStartBtn, repGraphStopBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        // Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Replication");
        xAxis.setForceZeroInRange(false);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Average time in system (s)");
        yAxis.setForceZeroInRange(false);

        repGraphChart = new LineChart<>(xAxis, yAxis);
        repGraphChart.setCreateSymbols(false);
        repGraphChart.setLegendVisible(true);
        repGraphChart.setAnimated(false);
        repGraphChart.setStyle(
                "-fx-background-color: " + COLOR_PANEL + ";" +
                        " -fx-text-fill: " + COLOR_TEXT + ";"
        );

        repGraphMeanSeries = new XYChart.Series<>();
        repGraphMeanSeries.setName("Mean");

        repGraphUpperSeries = new XYChart.Series<>();
        repGraphUpperSeries.setName("Mean + σ");

        repGraphLowerSeries = new XYChart.Series<>();
        repGraphLowerSeries.setName("Mean - σ");

        repGraphChart.getData().addAll(repGraphMeanSeries, repGraphUpperSeries, repGraphLowerSeries);

        root.getChildren().addAll(inputs, btnRow, repGraphChart);
        return root;
    }

    private void startReplicationGraphExperiment() {
        if (repGraphThread != null && repGraphThread.isAlive()) return;

        int reps;
        int capacity;
        try {
            reps = Integer.parseInt(repGraphReplField.getText());
            capacity = Integer.parseInt(repGraphCapacityField.getText());
        } catch (NumberFormatException ex) {
            return;
        }
        if (reps <= 0 || capacity <= 0) return;

        // Reset chart
        repGraphMeanSeries.getData().clear();
        repGraphUpperSeries.getData().clear();
        repGraphLowerSeries.getData().clear();

        repGraphRunning = true;
        repGraphStartBtn.setDisable(true);
        repGraphStopBtn.setDisable(false);

        repGraphThread = new Thread(() -> runReplicationGraphExperiment(reps, capacity));
        repGraphThread.setDaemon(true);
        repGraphThread.start();
    }

    private void stopReplicationGraphExperiment() {
        repGraphRunning = false;
        repGraphStopBtn.setDisable(true);
    }

    private void runReplicationGraphExperiment(int reps, int capacity) {
        // We create a new TerminalSimulation instance for each replication.
        // We then keep a running mean and variance over the replication means
        // and plot at most 1000 points.
        Random seedGen = new Random();
        int[] caps = parseFrontCapacities(repGraphBeforeXrayField, repGraphAfterXrayField);
        int beforeCap = caps[0];
        int afterCap = caps[1];
        MeanStat stat = new MeanStat();
        boolean showBands = false;


        for (int rep = 1; rep <= reps && repGraphRunning; rep++) {
            TerminalSimulation expSim = new TerminalSimulation();
            expSim.changeSleepTime(-1);
            expSim.startSimulation(1, capacity, beforeCap, afterCap);

            // mean time in system for this replication (in hours)
            double fRep = rep;
            stat.add(expSim.getCombinedAverageTimeInSim());
            double fMean = stat.getMean();
            double fUpper;
            double fLower;
            if (stat.getVariance() != -1) {
                showBands = true;

                fUpper = stat.getMean() + stat.getVariance();
                fLower = stat.getMean() - stat.getVariance();
            } else {
                fLower = 0.0;
                fUpper = 0.0;
            }

            boolean finalShowBands = showBands;
            if (rep > reps * 0.01) { // skip first 1% of points to let the graph "settle"
                Platform.runLater(() -> {
                    repGraphMeanSeries.getData().add(new XYChart.Data<>(fRep, fMean));
                    if (finalShowBands) {
                        repGraphUpperSeries.getData().add(new XYChart.Data<>(fRep, fUpper));
                        repGraphLowerSeries.getData().add(new XYChart.Data<>(fRep, fLower));
                    }
                });
            }
        }

        Platform.runLater(() -> {
            repGraphRunning = false;
            repGraphStartBtn.setDisable(false);
            repGraphStopBtn.setDisable(true);
        });
    }

    // =====================================================================
    // Helper view classes for interactive mode
    // =====================================================================

    private static class FrontView {
        private final Label titleLabel;
        private final Label sizeLabel;
        private final Label avgWaitLabel;
        private final Label avgLenLabel;
        private final ListView<String> listView;

        public FrontView(String title) {
            this.titleLabel = sectionHeaderStatic(title);
            this.sizeLabel = valueLabelStatic("0");
            this.avgWaitLabel = valueLabelStatic("—");
            this.avgLenLabel = valueLabelStatic("—");
            this.listView = new ListView<>();
            styleListView(listView);
            listView.setMinHeight(24);
            listView.setPrefHeight(Region.USE_COMPUTED_SIZE);
            listView.setMaxHeight(Region.USE_COMPUTED_SIZE);
        }

        public void addToGrid(GridPane grid, int col, int[] rowRef) {
            int row = rowRef[0];
            grid.add(titleLabel, col, row++, 2, 1);
            grid.add(captionLabelStatic("Size:"), col, row);
            grid.add(sizeLabel, col + 1, row++);
            grid.add(captionLabelStatic("Avg wait:"), col, row);
            grid.add(avgWaitLabel, col + 1, row++);
            grid.add(captionLabelStatic("Avg length:"), col, row);
            grid.add(avgLenLabel, col + 1, row++);
            grid.add(listView, col, row++, 2, 1);
            rowRef[0] = row;
        }

        public void update(FrontSnapshot snap) {
            sizeLabel.setText(String.valueOf(snap.size()));
            if (snap.avgWaitTime() > 0.0) {
                avgWaitLabel.setText(TimeParser.parseTime(snap.avgWaitTime()));
            } else {
                avgWaitLabel.setText("—");
            }
            if (snap.avgLength() > 0.0) {
                avgLenLabel.setText(String.format("%.3f", snap.avgLength()));
            } else {
                avgLenLabel.setText("—");
            }
            listView.getItems().setAll(snap.entities());
            int rows = Math.max(1, snap.entities().size());
            double rowHeight = 22.0;
            listView.setPrefHeight(rows * rowHeight + 4);
        }
    }

    private static class ServiceView {
        private final Label titleLabel;
        private final Label statusLabel;
        private final Label currentLabel;
        private final Label endTimeLabel;
        private final FrontView frontView;

        public ServiceView(String title, String frontTitle) {
            this.titleLabel = sectionHeaderStatic(title);
            this.statusLabel = valueLabelStatic("Idle");
            this.currentLabel = valueLabelStatic("—");
            this.endTimeLabel = valueLabelStatic("—");
            this.frontView = new FrontView(frontTitle);
        }

        public void addToGrid(GridPane grid, int col, int[] rowRef) {
            int row = rowRef[0];
            grid.add(titleLabel, col, row++, 2, 1);
            grid.add(captionLabelStatic("Status:"), col, row);
            grid.add(statusLabel, col + 1, row++);
            grid.add(captionLabelStatic("Current:"), col, row);
            grid.add(currentLabel, col + 1, row++);
            grid.add(captionLabelStatic("Ends at:"), col, row);
            grid.add(endTimeLabel, col + 1, row++);
            rowRef[0] = row;

            frontView.addToGrid(grid, col, rowRef);
        }

        public void update(ServiceSnapshot snap) {
            if (snap.busy()) {
                statusLabel.setText("Busy");
                statusLabel.setStyle("-fx-text-fill: " + COLOR_ERROR + "; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("Idle");
                statusLabel.setStyle("-fx-text-fill: " + COLOR_SUCCESS + "; -fx-font-weight: bold;");
            }
            currentLabel.setText(snap.currentEntity());
            if (snap.endServiceTime() > 0.0) {
                endTimeLabel.setText(TimeParser.parseTime(snap.endServiceTime()));
            } else {
                endTimeLabel.setText("—");
            }
            frontView.update(snap.frontSnapshot());
        }
    }

    private static class TerminalView {
        private final String name;
        private final FrontView placeFrontView;
        private final ServiceView xrayServiceView;
        private final ServiceView detectorServiceView;
        private final FrontView afterXrayFrontView;
        private final FrontView beforeTakeFrontView;
        private GridPane pane;

        public TerminalView(String name) {
            this.name = name;
            this.placeFrontView = new FrontView("Place crate front");
            this.xrayServiceView = new ServiceView("X-ray service", "X-ray front");
            this.detectorServiceView = new ServiceView("Detector service", "Detector front");
            this.afterXrayFrontView = new FrontView("After X-ray front");
            this.beforeTakeFrontView = new FrontView("Before take crate front");
        }

        public GridPane buildPane() {
            pane = new GridPane();
            pane.setHgap(8);
            pane.setVgap(4);
            pane.setPadding(new Insets(8));
            pane.setStyle("-fx-background-color: " + COLOR_PANEL_DARK + "; -fx-background-radius: 8;");

            int[] rowRef = new int[]{0};

            Label title = sectionHeaderStatic(name);
            pane.add(title, 0, rowRef[0]++, 2, 1);

            placeFrontView.addToGrid(pane, 0, rowRef);
            xrayServiceView.addToGrid(pane, 0, rowRef);
            detectorServiceView.addToGrid(pane, 0, rowRef);
            afterXrayFrontView.addToGrid(pane, 0, rowRef);
            beforeTakeFrontView.addToGrid(pane, 0, rowRef);

            return pane;
        }

        public void updateFromSnapshots(TerminalViewSnapshots snaps) {
            placeFrontView.update(snaps.placeFront());
            xrayServiceView.update(snaps.xrayService());
            detectorServiceView.update(snaps.detectorService());
            afterXrayFrontView.update(snaps.afterXrayFront());
            beforeTakeFrontView.update(snaps.beforeTakeFront());
        }
    }

    // =====================================================================
    // Slider + style helpers
    // =====================================================================

    private void applyGlobalDarkTheme(Scene scene, TabPane tabs) {
        scene.getRoot().setStyle(
                "-fx-base: " + COLOR_PANEL + ";" +
                        "-fx-background: " + COLOR_BG + ";" +
                        "-fx-control-inner-background: " + COLOR_PANEL + ";" +
                        "-fx-accent: " + COLOR_ACCENT + ";" +
                        "-fx-focus-color: " + COLOR_ACCENT + ";" +
                        "-fx-faint-focus-color: transparent;" +
                        "-fx-text-fill: " + COLOR_TEXT + ";"
        );

        String css = """
                .root {
                    -fx-background-color: %s;
                    -fx-text-fill: %s;
                }
                .tab-pane {
                    -fx-background-color: %s;
                }
                .tab-pane > .tab-header-area > .tab-header-background {
                    -fx-background-color: %s;
                }
                .tab {
                    -fx-background-color: %s;
                    -fx-background-radius: 8 8 0 0;
                    -fx-background-insets: 0;
                    -fx-padding: 8 14 8 14;
                }
                .tab:selected {
                    -fx-background-color: %s;
                }
                .tab .tab-label {
                    -fx-text-fill: %s;
                    -fx-font-weight: bold;
                }
                .scroll-pane, .scroll-pane > .viewport {
                    -fx-background-color: transparent;
                }
                .scroll-bar:horizontal, .scroll-bar:vertical {
                    -fx-background-color: %s;
                }
                .scroll-bar .thumb {
                    -fx-background-color: %s;
                    -fx-background-radius: 999;
                }
                .table-view {
                    -fx-background-color: %s;
                    -fx-control-inner-background: %s;
                    -fx-selection-bar: %s;
                    -fx-selection-bar-non-focused: %s;
                    -fx-table-cell-border-color: transparent;
                }
                .table-view .column-header-background,
                .table-view .column-header,
                .table-view .filler {
                    -fx-background-color: %s;
                    -fx-border-color: %s;
                }
                .table-view .column-header .label,
                .table-view .table-cell {
                    -fx-text-fill: %s;
                }
                .table-row-cell {
                    -fx-background-color: %s;
                }
                .table-row-cell:odd {
                    -fx-background-color: %s;
                }
                .table-row-cell:selected .text,
                .list-cell:selected .text {
                    -fx-fill: %s;
                }
                .list-view {
                    -fx-background-color: %s;
                    -fx-control-inner-background: %s;
                    -fx-border-color: %s;
                }
                .list-cell {
                    -fx-background-color: %s;
                    -fx-text-fill: %s;
                }
                .list-cell:filled:selected, .list-cell:filled:hover {
                    -fx-background-color: %s;
                }
                .chart {
                    -fx-background-color: %s;
                    -fx-padding: 10;
                }
                .chart-plot-background {
                    -fx-background-color: %s;
                }
                .chart-title,
                .chart-legend,
                .chart-legend-item,
                .chart-legend-item .label,
                .axis-label {
                    -fx-text-fill: %s;
                }
                .axis {
                    -fx-tick-label-fill: %s;
                }
                .axis:top, .axis:right, .axis:bottom, .axis:left {
                    -fx-border-color: %s;
                }
                .chart-vertical-grid-lines,
                .chart-horizontal-grid-lines,
                .chart-alternative-row-fill,
                .chart-alternative-column-fill {
                    -fx-stroke: %s;
                }
                .default-color0.chart-series-line {
                    -fx-stroke: %s;
                    -fx-stroke-width: 2px;
                }
                .default-color0.chart-line-symbol {
                    -fx-background-color: %s, %s;
                }
                .check-box .box {
                    -fx-background-color: %s;
                    -fx-border-color: %s;
                }
                .check-box:selected .mark {
                    -fx-background-color: %s;
                }
                .label {
                    -fx-text-fill: %s;
                }
                """.formatted(
                COLOR_BG, COLOR_TEXT,
                COLOR_BG,
                COLOR_PANEL_DARK,
                COLOR_PANEL,
                COLOR_PANEL_DARK,
                COLOR_TEXT,
                COLOR_PANEL_DARK,
                COLOR_BORDER,
                COLOR_PANEL, COLOR_PANEL,
                COLOR_SELECTION, COLOR_SELECTION,
                COLOR_HEADER, COLOR_BORDER,
                COLOR_TEXT,
                COLOR_PANEL,
                COLOR_PANEL_DARK,
                COLOR_TEXT,
                COLOR_INPUT, COLOR_INPUT, COLOR_BORDER,
                COLOR_INPUT, COLOR_TEXT,
                COLOR_SELECTION,
                COLOR_PANEL,
                COLOR_PANEL_DARK,
                COLOR_TEXT,
                COLOR_TEXT_DIM,
                COLOR_BORDER,
                COLOR_BORDER,
                COLOR_ACCENT,
                COLOR_ACCENT, COLOR_PANEL_DARK,
                COLOR_INPUT, COLOR_BORDER,
                COLOR_BG,
                COLOR_TEXT
        );

        scene.getStylesheets().add("data:text/css," + URLEncoder.encode(css, StandardCharsets.UTF_8));

        if (tabs != null) {
            for (Tab tab : tabs.getTabs()) {
                if (tab.getContent() != null) {
                    applyTextVisibility(tab.getContent());
                }
            }
        }
    }

    private static void applyTextVisibility(Node node) {
        if (node instanceof Labeled labeled) {
            labeled.setTextFill(Color.web(COLOR_TEXT));
        }
        if (node instanceof TextInputControl input) {
            input.setStyle(input.getStyle() + "-fx-prompt-text-fill: " + COLOR_TEXT_DIM + "; -fx-highlight-fill: " + COLOR_ACCENT + "; -fx-highlight-text-fill: black;");
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyTextVisibility(child);
            }
        }
    }

    private static void styleScrollPane(ScrollPane scroll) {
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    }

    private static void styleTable(TableView<SweepRow> table) {
        table.setStyle(
                "-fx-background-color: " + COLOR_PANEL + ";" +
                        "-fx-control-inner-background: " + COLOR_PANEL + ";" +
                        "-fx-selection-bar: " + COLOR_SELECTION + ";" +
                        "-fx-selection-bar-non-focused: " + COLOR_SELECTION + ";" +
                        "-fx-text-background-color: " + COLOR_TEXT + ";" +
                        "-fx-table-cell-border-color: transparent;" +
                        "-fx-table-header-border-color: " + COLOR_BORDER + ";"
        );
    }

    private static void styleChart(LineChart<Number, Number> chart, NumberAxis xAxis, NumberAxis yAxis) {
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(true);
        chart.setStyle("-fx-background-color: " + COLOR_PANEL + ";");
        xAxis.setTickLabelFill(Color.web(COLOR_TEXT_DIM));
        yAxis.setTickLabelFill(Color.web(COLOR_TEXT_DIM));
        xAxis.setStyle("-fx-text-fill: " + COLOR_TEXT + "; -fx-tick-label-fill: " + COLOR_TEXT_DIM + ";");
        yAxis.setStyle("-fx-text-fill: " + COLOR_TEXT + "; -fx-tick-label-fill: " + COLOR_TEXT_DIM + ";");
    }

    private static void styleListView(ListView<String> listView) {
        listView.setStyle(
                "-fx-background-color: " + COLOR_INPUT + ";" +
                        "-fx-control-inner-background: " + COLOR_INPUT + ";" +
                        "-fx-border-color: " + COLOR_BORDER + ";" +
                        "-fx-text-fill: " + COLOR_TEXT + ";"
        );
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web(COLOR_TEXT));
                setBackground(new Background(new BackgroundFill(Color.web(empty ? COLOR_INPUT : COLOR_INPUT), CornerRadii.EMPTY, Insets.EMPTY)));
            }
        });
    }

    private int mapSliderToSleep(int sliderVal) {
        return 250 - sliderVal;
    }

    private String sliderDescription(int sliderVal) {
        int sleep = mapSliderToSleep(sliderVal);
        if (sleep == -1) {
            return "Max speed";
        }
        if (sleep == 250) {
            return "Real-time (1×)";
        }
        return String.format("%.1f× speed", 250.0 / sleep);
    }

    private void styleCheckBox(CheckBox cb) {
        cb.setStyle("-fx-text-fill: " + COLOR_TEXT + ";");
    }

    private void styleInput(Control c) {
        c.setStyle("-fx-background-color: #313244; -fx-text-fill: " + COLOR_TEXT +
                "; -fx-border-color: " + COLOR_BORDER + "; -fx-border-radius: 4;");
    }

    private void styleButton(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 6;");
        btn.setPrefWidth(120);
    }

    private Label styledLabel(String text, String color, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + ";");
        return l;
    }

    private Label captionLabel(String text) {
        return styledLabel(text, COLOR_TEXT_DIM, 12);
    }

    private Label valueLabel(String text) {
        Label l = styledLabel(text, COLOR_TEXT, 13);
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        return l;
    }

    private Label sectionHeader(String text) {
        Label l = styledLabel(text, COLOR_ACCENT, 13);
        l.setFont(Font.font(null, FontWeight.BOLD, 13));
        return l;
    }

    // static versions for nested view classes
    private static Label captionLabelStatic(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + COLOR_TEXT_DIM + "; -fx-font-size: 12;");
        return l;
    }

    private static Label valueLabelStatic(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + COLOR_TEXT + "; -fx-font-size: 13;");
        l.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        return l;
    }

    private static Label sectionHeaderStatic(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 13;");
        l.setFont(Font.font(null, FontWeight.BOLD, 13));
        return l;
    }

    private int[] parseFrontCapacities(TextField beforeField, TextField afterField) {
        int before = 4;
        int after = 5;
        try {
            before = Integer.parseInt(beforeField.getText());
            after = Integer.parseInt(afterField.getText());
        } catch (NumberFormatException ignored) { }
        before = Math.max(2, before);
        after = Math.max(2, after);
        // reflect clamped values back into the fields
        beforeField.setText(String.valueOf(before));
        afterField.setText(String.valueOf(after));
        return new int[]{before, after};
    }

    public static void main(String[] args) {
        launch(args);
    }
}