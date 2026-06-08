package sk.uniza.adamec2.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import sk.uniza.adamec2.sem_one.SemMonteCarlo;
import sk.uniza.adamec2.util.TimeParser;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/*
 * Vygenerované pomocou AI, v dokumentácií kapitola 3
 */
public class SimulationGUI extends Application implements SimulationListener {

    // ── Experiment modes ──────────────────────────────────────────────────────
    private enum ExperimentMode { SINGLE, GRID, PROBABILITY }
    private ExperimentMode currentMode = ExperimentMode.SINGLE;

    // ── Controls ──────────────────────────────────────────────────────────────
    private ComboBox<String> orderComboBox;
    private TextField replicationsField;
    private TextField startPercentField;
    private Button startButton, stopButton;
    private RadioButton singleRadio, gridRadio, probRadio;

    // Start / slowdown time
    private TextField startTimeField, slowdownStartTimeField;
    private CheckBox startTimeCheckBox, slowdownStartTimeCheckBox;
    private double startTime = 6.0;
    private double slowdownStartTime = 6.5;

    // Probability experiment extra controls
    private HBox probControlsBox;
    private TextField endUntilTimeField;
    private CheckBox endUntilTimeCheckBox;
    private TextField targetPercentField;
    private CheckBox targetPercentCheckBox;
    private double endUntilTime = 7.0 + 35.0 / 60.0;
    private double targetPercent = 0.8;

    // ── Single chart ──────────────────────────────────────────────────────────
    private LineChart<Number, Number> chart;
    private NumberAxis yAxis, xAxis;
    private XYChart.Series<Number, Number> series;

    // ── Grid view ─────────────────────────────────────────────────────────────
    private GridPane gridPane;
    private LineChart<Number, Number>[] gridCharts;
    private XYChart.Series<Number, Number>[] gridSeries;
    private NumberAxis[] gridYAxes, gridXAxes;
    private Label[] gridLabels;
    private double[] minYObservedGrid, maxYObservedGrid;
    private double[] gridFinalAverages;
    private int currentGridIndex = 0;

    // ── Probability experiment ────────────────────────────────────────────────
    private VBox probResultBox;
    private Label experimentResultLabel;
    private Label optimalLabel;

    // ── Shared chart container ────────────────────────────────────────────────
    private StackPane chartContainer;
    private Label avgLabel;

    // ── Simulation state ──────────────────────────────────────────────────────
    private SemMonteCarlo simulation;
    private Thread simulationThread, uiUpdateThread;
    private int totalReplications, graphStartReplication, graphUpdateInterval;
    private double minYObserved = Double.MAX_VALUE;
    private double maxYObserved = Double.MIN_VALUE;
    private volatile boolean simulationRunning = false;

    // Thread-safe data holders
    private final AtomicReference<Double> currentAverage = new AtomicReference<>(0.0);
    private final List<DataPoint> pendingPoints = new ArrayList<>();
    private final Object pointsLock = new Object();

    private static final String[] ALL_ORDERS = {
            "Z→S→R→D→Z", "Z→S→D→R→Z", "Z→R→S→D→Z",
            "Z→R→D→S→Z", "Z→D→S→R→Z", "Z→D→R→S→Z"
    };

    private static class DataPoint {
        int replication;
        double value;
        DataPoint(int replication, double value) {
            this.replication = replication;
            this.value = value;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        // ── Row 1: route / replications / graph % / start time / slowdown ────
        HBox controls1 = new HBox(10);
        controls1.setAlignment(Pos.CENTER_LEFT);

        orderComboBox = new ComboBox<>();
        orderComboBox.getItems().addAll(ALL_ORDERS);
        orderComboBox.setValue("Z→S→R→D→Z");
        orderComboBox.setPrefWidth(150);

        replicationsField = new TextField("10000000");
        replicationsField.setPrefWidth(100);

        startPercentField = new TextField("40");
        startPercentField.setPrefWidth(60);

        // Start time
        startTimeField = new TextField("6:00:00");
        startTimeField.setPrefWidth(60);
        startTimeField.setDisable(true);
        startTimeCheckBox = new CheckBox();
        startTimeCheckBox.selectedProperty().addListener((obs, was, is) -> {
            startTimeField.setDisable(!is);
            if (!is) startTimeField.setText("6:00:00");
        });

        // Slowdown start time
        slowdownStartTimeField = new TextField("6:30:00");
        slowdownStartTimeField.setPrefWidth(60);
        slowdownStartTimeField.setDisable(true);
        slowdownStartTimeCheckBox = new CheckBox();
        slowdownStartTimeCheckBox.selectedProperty().addListener((obs, was, is) -> {
            slowdownStartTimeField.setDisable(!is);
            if (!is) slowdownStartTimeField.setText("6:30:00");
        });

        controls1.getChildren().addAll(
                new Label("Route:"), orderComboBox,
                new Label("Replications:"), replicationsField,
                new Label("Start Graph at %:"), startPercentField,
                new Label("Start Time:"), startTimeCheckBox, startTimeField,
                new Label("Slowdown Start:"), slowdownStartTimeCheckBox, slowdownStartTimeField
        );

        // ── Row 2: mode radio buttons + action buttons ────────────────────────
        HBox controls2 = new HBox(10);
        controls2.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup modeGroup = new ToggleGroup();
        singleRadio = new RadioButton("Single Chart");
        gridRadio   = new RadioButton("Grid View (All Routes)");
        probRadio   = new RadioButton("Probability Experiment");
        singleRadio.setToggleGroup(modeGroup);
        gridRadio.setToggleGroup(modeGroup);
        probRadio.setToggleGroup(modeGroup);
        singleRadio.setSelected(true);

        modeGroup.selectedToggleProperty().addListener((obs, old, nw) -> {
            if (!simulationRunning) switchMode();
        });

        startButton = new Button("Start Simulation");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        stopButton = new Button("Stop");
        stopButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        stopButton.setDisable(true);

        controls2.getChildren().addAll(
                singleRadio, gridRadio, probRadio,
                new Separator(), startButton, stopButton
        );

        // ── Row 3: probability experiment extra fields (hidden initially) ─────
        probControlsBox = new HBox(10);
        probControlsBox.setAlignment(Pos.CENTER_LEFT);
        probControlsBox.setVisible(false);
        probControlsBox.setManaged(false);

        endUntilTimeField = new TextField("7:35:00");
        endUntilTimeField.setPrefWidth(70);
        endUntilTimeField.setDisable(true);
        endUntilTimeCheckBox = new CheckBox();
        endUntilTimeCheckBox.selectedProperty().addListener((obs, was, is) -> {
            endUntilTimeField.setDisable(!is);
            if (!is) endUntilTimeField.setText("7:35:00");
        });

        targetPercentField = new TextField("80.0");
        targetPercentField.setPrefWidth(60);
        targetPercentField.setDisable(true);
        targetPercentCheckBox = new CheckBox();
        targetPercentCheckBox.selectedProperty().addListener((obs, was, is) -> {
            targetPercentField.setDisable(!is);
            if (!is) targetPercentField.setText("80.0");
        });

        probControlsBox.getChildren().addAll(
                new Label("Finish Until:"), endUntilTimeCheckBox, endUntilTimeField,
                new Label("Target % of replications:"), targetPercentCheckBox, targetPercentField
        );

        // ── Initialise chart areas ────────────────────────────────────────────
        initializeSingleChart();
        initializeGridView();
        initializeProbView();

        chartContainer = new StackPane();
        chartContainer.getChildren().add(chart);   // default: single chart
        chartContainer.setPrefHeight(700);

        avgLabel = new Label("Average Time: Not started");
        avgLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        root.getChildren().addAll(controls1, controls2, probControlsBox, chartContainer, avgLabel);

        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> stopSimulation());

        simulation = new SemMonteCarlo();
        simulation.setListener(this);

        Scene scene = new Scene(root, 1400, 1000);
        primaryStage.setTitle("Monte Carlo Simulation - Courier Route Optimization");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            simulationRunning = false;
            if (simulation != null) simulation.setSimulationRunning(false);
        });
        primaryStage.show();
    }

    // ── Mode switching ────────────────────────────────────────────────────────
    private void switchMode() {
        if (singleRadio.isSelected()) {
            currentMode = ExperimentMode.SINGLE;
        } else if (gridRadio.isSelected()) {
            currentMode = ExperimentMode.GRID;
        } else {
            currentMode = ExperimentMode.PROBABILITY;
        }

        chartContainer.getChildren().clear();
        probControlsBox.setVisible(false);
        probControlsBox.setManaged(false);
        orderComboBox.setDisable(false);
        avgLabel.setText("Not started");

        switch (currentMode) {
            case SINGLE -> chartContainer.getChildren().add(chart);
            case GRID -> {
                chartContainer.getChildren().add(gridPane);
                orderComboBox.setDisable(true);
                avgLabel.setText("Grid View: all routes will be simulated sequentially");
            }
            case PROBABILITY -> {
                chartContainer.getChildren().add(probResultBox);
                probControlsBox.setVisible(true);
                probControlsBox.setManaged(true);
                avgLabel.setText("Probability Experiment: configure parameters and start");
            }
        }
    }

    // ── Chart initializers ────────────────────────────────────────────────────
    private void initializeSingleChart() {
        xAxis = new NumberAxis();
        xAxis.setLabel("Replication Number");
        xAxis.setAutoRanging(false);
        yAxis = new NumberAxis();
        yAxis.setLabel("Average Time (h)");
        yAxis.setAutoRanging(false);
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Monte Carlo Simulation");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        series = new XYChart.Series<>();
        chart.getData().add(series);
    }

    @SuppressWarnings("unchecked")
    private void initializeGridView() {
        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(5));

        gridCharts = new LineChart[6];
        gridSeries = new XYChart.Series[6];
        gridYAxes  = new NumberAxis[6];
        gridXAxes  = new NumberAxis[6];
        gridLabels = new Label[6];
        minYObservedGrid = new double[6];
        maxYObservedGrid = new double[6];
        gridFinalAverages = new double[6];
        Arrays.fill(gridFinalAverages, Double.NaN);
        Arrays.fill(minYObservedGrid, Double.MAX_VALUE);
        Arrays.fill(maxYObservedGrid, Double.MIN_VALUE);

        for (int i = 0; i < 6; i++) {
            gridXAxes[i] = new NumberAxis();
            gridXAxes[i].setLabel("Replication");
            gridXAxes[i].setAutoRanging(false);
            gridYAxes[i] = new NumberAxis();
            gridYAxes[i].setLabel("Avg Time (h)");
            gridYAxes[i].setAutoRanging(false);

            gridCharts[i] = new LineChart<>(gridXAxes[i], gridYAxes[i]);
            gridCharts[i].setTitle(ALL_ORDERS[i]);
            gridCharts[i].setCreateSymbols(false);
            gridCharts[i].setAnimated(false);
            gridCharts[i].setLegendVisible(false);

            gridSeries[i] = new XYChart.Series<>();
            gridCharts[i].getData().add(gridSeries[i]);

            gridLabels[i] = new Label("Not simulated yet");
            gridLabels[i].setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
            gridLabels[i].setAlignment(Pos.CENTER);
            gridLabels[i].setMaxWidth(Double.MAX_VALUE);

            VBox box = new VBox(4, gridCharts[i], gridLabels[i]);
            box.setAlignment(Pos.CENTER);
            gridPane.add(box, i % 3, i / 3);
        }
    }

    private void initializeProbView() {
        experimentResultLabel = new Label("Run the experiment to see results.");
        experimentResultLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        experimentResultLabel.setWrapText(true);

        optimalLabel = new Label("");
        optimalLabel.setStyle("-fx-font-size: 16px;");
        optimalLabel.setWrapText(true);

        probResultBox = new VBox(20, experimentResultLabel, optimalLabel);
        probResultBox.setAlignment(Pos.CENTER);
        probResultBox.setPadding(new Insets(40));
    }

    // ── Start ─────────────────────────────────────────────────────────────────
    private void startSimulation() {
        try {
            totalReplications = Integer.parseInt(replicationsField.getText());
            if (totalReplications <= 0) { showError("Replications must be positive"); return; }

            int startPercent = Integer.parseInt(startPercentField.getText());
            if (startPercent < 0 || startPercent >= 100) {
                showError("Start percentage must be between 0 and 99"); return;
            }

            startTime = TimeParser.parseTime(startTimeField.getText());
            slowdownStartTime = TimeParser.parseTime(slowdownStartTimeField.getText());

            if (currentMode == ExperimentMode.PROBABILITY) {
                endUntilTime  = TimeParser.parseTime(endUntilTimeField.getText());
                double rawPct = Double.parseDouble(targetPercentField.getText());
                if (rawPct <= 0 || rawPct >= 100) {
                    showError("Target % must be between 0 and 100"); return;
                }
                targetPercent = rawPct / 100.0;
            }

            graphStartReplication = (int) (totalReplications * (startPercent / 100.0));
            int afterStart = totalReplications - graphStartReplication;
            graphUpdateInterval = Math.max(1, afterStart / 1000);

        } catch (NumberFormatException e) {
            showError("Invalid input values."); return;
        } catch (IllegalArgumentException e) {
            showError(e.getMessage()); return;
        }

        clearAllGraphs();

        switch (currentMode) {
            case SINGLE      -> startSingleSimulation();
            case GRID        -> startGridSimulation();
            case PROBABILITY -> startProbSimulation();
        }
    }

    private void clearAllGraphs() {
        Platform.runLater(() -> {
            series.getData().clear();
            series.getData().add(new XYChart.Data<>(0, 0));
            experimentResultLabel.setText("Calculating...");
            optimalLabel.setText("");
        });

        minYObserved = Double.MAX_VALUE;
        maxYObserved = Double.MIN_VALUE;

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            Platform.runLater(() -> {
                gridSeries[idx].getData().clear();
                gridSeries[idx].getData().add(new XYChart.Data<>(0, 0));
                gridLabels[idx].setText("Not simulated yet");
                gridLabels[idx].setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;");
            });
            minYObservedGrid[i] = Double.MAX_VALUE;
            maxYObservedGrid[i] = Double.MIN_VALUE;
        }
        Arrays.fill(gridFinalAverages, Double.NaN);

    }

    // ── Single simulation ─────────────────────────────────────────────────────
    private void startSingleSimulation() {
        xAxis.setLowerBound(graphStartReplication);
        xAxis.setUpperBound(totalReplications);
        xAxis.setTickUnit((totalReplications - graphStartReplication) / 10.0);
        resetAtomicVariables();
        disableControls();
        int[] order = parseOrder(orderComboBox.getValue());
        startUIUpdateThread();
        startSimulationThread(order, false, 0, 0);
    }

    // ── Grid simulation ───────────────────────────────────────────────────────
    private void startGridSimulation() {
        for (int i = 0; i < 6; i++) {
            gridXAxes[i].setLowerBound(graphStartReplication);
            gridXAxes[i].setUpperBound(totalReplications);
            gridXAxes[i].setTickUnit((totalReplications - graphStartReplication) / 5.0);
            minYObservedGrid[i] = Double.MAX_VALUE;
            maxYObservedGrid[i] = Double.MIN_VALUE;
        }
        currentGridIndex = 0;
        disableControls();
        avgLabel.setText("Running simulations for all routes...");
        startNextGridSimulation();
    }

    private void startNextGridSimulation() {
        if (currentGridIndex >= 6) {
            Platform.runLater(() -> { avgLabel.setText("All route simulations completed!"); resetUI(); });
            return;
        }
        resetAtomicVariables();
        int[] order = parseOrder(ALL_ORDERS[currentGridIndex]);
        Platform.runLater(() -> {
            avgLabel.setText(String.format("Simulating route %d/6: %s", currentGridIndex + 1, ALL_ORDERS[currentGridIndex]));
            gridLabels[currentGridIndex].setText("Calculating...");
            gridLabels[currentGridIndex].setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF8C00;");
        });
        startUIUpdateThread();
        startSimulationThread(order, false, 0, 0);
    }

    private void displayGridRanking() {
        // collect only completed (non-NaN) entries
        List<int[]> completed = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (!Double.isNaN(gridFinalAverages[i])) {
                completed.add(new int[]{i});
            }
        }
        // sort by average ascending
        completed.sort((a, b) -> Double.compare(gridFinalAverages[a[0]], gridFinalAverages[b[0]]));

        if (completed.isEmpty()) {
            avgLabel.setText("No routes completed.");
            return;
        }

        StringBuilder sb = new StringBuilder("Ranking (fastest→slowest): ");
        for (int rank = 0; rank < completed.size(); rank++) {
            int idx = completed.get(rank)[0];
            sb.append(String.format("%d. %s (%s)",
                    rank + 1,
                    ALL_ORDERS[idx],
                    TimeParser.parseTime(gridFinalAverages[idx])));
            if (rank < completed.size() - 1) sb.append("  |  ");
        }
        avgLabel.setText(sb.toString());
    }


    // ── Probability simulation ────────────────────────────────────────────────
    private void startProbSimulation() {
        resetAtomicVariables();
        disableControls();
        avgLabel.setText("Probability Experiment: running...");
        Platform.runLater(() -> {
            experimentResultLabel.setText("Calculating...");
            optimalLabel.setText("");
        });
        startUIUpdateThread();
        int[] order = parseOrder(orderComboBox.getValue());
        startSimulationThread(order, true, targetPercent, endUntilTime);
    }

    private void updateOptimalLabel(double pct) {
        double targetPct = targetPercent * 100;
        String optText;
        if (pct >= targetPct && pct <= targetPct + 0.1) {
            optText = String.format("✔ Start time %s is near-optimal for %.2f%% target (within +0.1%%)",
                    TimeParser.parseTime(startTime), targetPct);
            optimalLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");
        } else {
            optText = String.format("✘ Result %.4f%% differs from target %.2f%% by %.4f%%",
                    pct, targetPct, Math.abs(pct - targetPct));
            optimalLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #f44336;");
        }
        optimalLabel.setText(optText);
        avgLabel.setText("Probability Experiment complete.");
    }



    // ── Generic simulation thread launcher ────────────────────────────────────
    private void startSimulationThread(int[] order, boolean isU2, double moreThanPercent, double endUntil) {
        simulationThread = new Thread(() -> {
            try {
                if (isU2) {
                    simulation.startNewSimulation(order, totalReplications, startTime,
                            slowdownStartTime, endUntil);
                } else {
                    simulation.startNewSimulation(order, totalReplications, startTime, slowdownStartTime);
                }
            } catch (Exception e) {
                simulationRunning = false;
                Platform.runLater(() -> {
                    showError("Simulation error: " + e.getMessage());
                    e.printStackTrace();
                    resetUI();
                });
            }
        });
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    // ── UI update thread ──────────────────────────────────────────────────────
    private void startUIUpdateThread() {
        simulationRunning = true;
        uiUpdateThread = new Thread(() -> {
            while (simulationRunning) {
                try {
                    Thread.sleep(10);
                    Platform.runLater(() -> {
                        if (!simulationRunning) return;
                        double val = currentAverage.get();

                        if (currentMode == ExperimentMode.PROBABILITY) {
                            experimentResultLabel.setText(String.format(
                                    "%.4f%% of replications finish before %s",
                                    val * 100, TimeParser.parseTime(endUntilTime)));
                            avgLabel.setText("Probability Experiment: running...");
                        } else if (currentMode == ExperimentMode.SINGLE) {
                            avgLabel.setText("Average Time: " + TimeParser.parseTime(val));
                        } else if (currentGridIndex < 6) {
                            avgLabel.setText(String.format("Simulating route %d/6: %s — Current avg: %s",
                                    currentGridIndex + 1, ALL_ORDERS[currentGridIndex],
                                    TimeParser.parseTime(val)));
                        }

                        List<DataPoint> toAdd;
                        synchronized (pointsLock) {
                            toAdd = new ArrayList<>(pendingPoints);
                            pendingPoints.clear();
                        }

                        if (currentMode == ExperimentMode.GRID && currentGridIndex < 6) {
                            for (DataPoint p : toAdd) {
                                gridSeries[currentGridIndex].getData().add(new XYChart.Data<>(p.replication, p.value));
                                updateYAxisBoundsGrid(p.value, currentGridIndex);
                            }
                        } else if (currentMode == ExperimentMode.SINGLE) {
                            for (DataPoint p : toAdd) {
                                series.getData().add(new XYChart.Data<>(p.replication, p.value));
                                updateYAxisBounds(p.value);
                            }
                        }
                        // PROBABILITY mode: no chart to update
                    });
                } catch (InterruptedException e) { break; }
            }
        });
        uiUpdateThread.setDaemon(true);
        uiUpdateThread.start();
    }

    // ── Listener callbacks ────────────────────────────────────────────────────
    @Override
    public void onReplicationComplete(int replicationNumber, double value) {
        currentAverage.set(value);

        if (currentMode != ExperimentMode.PROBABILITY
                && replicationNumber >= graphStartReplication) {
            int fromStart = replicationNumber - graphStartReplication;
            if (fromStart % graphUpdateInterval == 0 || replicationNumber == totalReplications) {
                synchronized (pointsLock) {
                    pendingPoints.add(new DataPoint(replicationNumber, value));
                }
            }
        }
    }

    @Override
    public void onSimulationComplete(double finalValue) {
        simulationRunning = false;
        Platform.runLater(() -> {
            // flush remaining points
            List<DataPoint> toAdd;
            synchronized (pointsLock) {
                toAdd = new ArrayList<>(pendingPoints);
                pendingPoints.clear();
            }

            if (currentMode == ExperimentMode.PROBABILITY) {
                double pct = finalValue * 100;
                experimentResultLabel.setText(String.format("%.4f%% of replications finish before %s",
                        pct, TimeParser.parseTime(endUntilTime)));
                updateOptimalLabel(pct);
                resetUI();

            } else if (currentMode == ExperimentMode.GRID && currentGridIndex < 6) {
                for (DataPoint p : toAdd) {
                    gridSeries[currentGridIndex].getData().add(new XYChart.Data<>(p.replication, p.value));
                    updateYAxisBoundsGrid(p.value, currentGridIndex);
                }
                gridFinalAverages[currentGridIndex] = finalValue;
                gridLabels[currentGridIndex].setText("Avg: " + TimeParser.parseTime(finalValue));
                gridLabels[currentGridIndex].setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
                currentGridIndex++;
                if (currentGridIndex < 6) {
                    startNextGridSimulation();
                } else {
                    displayGridRanking();  // ← replaces avgLabel.setText("All route simulations completed!")
                    resetUI();
                }

            } else {
                for (DataPoint p : toAdd) {
                    series.getData().add(new XYChart.Data<>(p.replication, p.value));
                    updateYAxisBounds(p.value);
                }
                avgLabel.setText("Final Average Time: " + TimeParser.parseTime(finalValue));
                resetUI();
            }
        });
    }

    @Override
    public void onSimulationStopped(double currentVal, int completedReplications) {
        simulationRunning = false;
        Platform.runLater(() -> {
            if (currentMode == ExperimentMode.PROBABILITY) {
                double pct = currentVal * 100;
                experimentResultLabel.setText(String.format(
                        "Stopped — %.4f%% finished before %s (%d replications)",
                        pct, TimeParser.parseTime(endUntilTime), completedReplications));
                updateOptimalLabel(pct);
            } else if (currentMode == ExperimentMode.GRID && currentGridIndex < 6) {
                gridFinalAverages[currentGridIndex] = currentVal;
                gridLabels[currentGridIndex].setText("Stopped — Avg: " + TimeParser.parseTime(currentVal));
                gridLabels[currentGridIndex].setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #f44336;");
                displayGridRanking();
                resetUI();
                return;
            }
            avgLabel.setText(String.format("Stopped (%d replications) — %s",
                    completedReplications,
                    currentMode == ExperimentMode.PROBABILITY
                            ? String.format("%.4f%%", currentVal * 100)
                            : TimeParser.parseTime(currentVal)));
            resetUI();
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private void resetAtomicVariables() {
        currentAverage.set(0.0);
        synchronized (pointsLock) { pendingPoints.clear(); }
    }

    private void disableControls() {
        startButton.setDisable(true);
        stopButton.setDisable(false);
        singleRadio.setDisable(true);
        gridRadio.setDisable(true);
        probRadio.setDisable(true);
        orderComboBox.setDisable(true);
        replicationsField.setDisable(true);
        startPercentField.setDisable(true);
        startTimeCheckBox.setDisable(true);
        startTimeField.setDisable(true);
        slowdownStartTimeCheckBox.setDisable(true);
        slowdownStartTimeField.setDisable(true);
        endUntilTimeCheckBox.setDisable(true);
        endUntilTimeField.setDisable(true);
        targetPercentCheckBox.setDisable(true);
        targetPercentField.setDisable(true);
        avgLabel.setText("Calculating...");
    }

    private void resetUI() {
        startButton.setDisable(false);
        stopButton.setDisable(true);
        singleRadio.setDisable(false);
        gridRadio.setDisable(false);
        probRadio.setDisable(false);
        orderComboBox.setDisable(currentMode == ExperimentMode.GRID);
        replicationsField.setDisable(false);
        startPercentField.setDisable(false);
        startTimeCheckBox.setDisable(false);
        startTimeField.setDisable(!startTimeCheckBox.isSelected());
        slowdownStartTimeCheckBox.setDisable(false);
        slowdownStartTimeField.setDisable(!slowdownStartTimeCheckBox.isSelected());
        if (currentMode == ExperimentMode.PROBABILITY) {
            endUntilTimeCheckBox.setDisable(false);
            endUntilTimeField.setDisable(!endUntilTimeCheckBox.isSelected());
            targetPercentCheckBox.setDisable(false);
            targetPercentField.setDisable(!targetPercentCheckBox.isSelected());
        }
    }

    private void stopSimulation() {
        simulationRunning = false;
        if (simulation != null) simulation.setSimulationRunning(false);
        stopButton.setDisable(true);
    }

    private void updateYAxisBounds(double value) {
        boolean changed = false;
        if (value < minYObserved) { minYObserved = value; changed = true; }
        if (value > maxYObserved) { maxYObserved = value; changed = true; }
        if (changed) {
            double range = maxYObserved - minYObserved;
            double pad = range * 0.01;
            yAxis.setLowerBound(minYObserved - pad);
            yAxis.setUpperBound(maxYObserved + pad);
            yAxis.setTickUnit(range / 10);
        }
    }

    private void updateYAxisBoundsGrid(double value, int index) {
        boolean changed = false;
        if (value < minYObservedGrid[index]) { minYObservedGrid[index] = value; changed = true; }
        if (value > maxYObservedGrid[index]) { maxYObservedGrid[index] = value; changed = true; }
        if (changed) {
            double range = maxYObservedGrid[index] - minYObservedGrid[index];
            double pad = range * 0.01;
            gridYAxes[index].setLowerBound(minYObservedGrid[index] - pad);
            gridYAxes[index].setUpperBound(maxYObservedGrid[index] + pad);
            gridYAxes[index].setTickUnit(range / 10);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int[] parseOrder(String orderStr) {
        Map<String, Integer> cityMap = Map.of("Z", 0, "S", 1, "R", 2, "D", 3);
        String[] cities = orderStr.split("→");
        return Arrays.stream(cities).mapToInt(cityMap::get).toArray();
    }

    public static void main(String[] args) { launch(args); }
}
