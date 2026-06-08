# Event-Driven & Monte Carlo Simulation

A university project for the **Discrete Simulation (DIS)** course at UNIZA. The system implements a custom event-driven simulation engine from scratch in Java, extended with Monte Carlo methods for probabilistic analysis. A JavaFX GUI provides real-time visualization of the simulation state and statistics.

## Overview

The project is split into two semesters:

- **Semester 1 (`sem_one`)** — Core event-driven simulation engine with a queueing/service model
- **Semester 2 (`sem_two`)** — Extension with Monte Carlo simulation and statistical analysis

Both simulations share a reusable `core` engine, custom random `generator` distributions, and a `stat` module for online statistics (mean, variance, confidence intervals).

## Architecture

```
src/main/java/sk/uniza/adamec2/
├── core/           # Event-driven simulation kernel (event queue, simulation clock)
├── generator/      # Custom probability distribution generators (uniform, exponential, ...)
├── stat/           # Online statistics: mean, variance, confidence intervals
├── util/           # Shared utilities
├── gui/            # JavaFX controllers and FXML views
├── sem_one/        # Semester 1 — event-driven queueing simulation
├── sem_two/        # Semester 2 — Monte Carlo simulation
└── Main.java       # Application entry point
```

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Core language |
| JavaFX | 21.0.6 | GUI / real-time visualization |
| Maven | 3.x | Build & dependency management |
| JUnit 5 | 5.12.1 | Unit testing |

## Key Concepts Implemented

- **Event-driven simulation** — custom priority queue-based event scheduler, simulation clock
- **Monte Carlo method** — probabilistic estimation using repeated random sampling
- **Custom RNG distributions** — uniform, exponential, empirical (not relying on `Math.random()`)
- **Online statistics** — Welford's algorithm for running mean/variance without storing all samples
- **Confidence intervals** — Student's t-distribution for result confidence estimation
- **Real-time GUI** — JavaFX charts updating live during simulation execution

## Getting Started

### Prerequisites

- JDK 21+ (project targets Java 25, JDK 21 LTS recommended)
- Maven 3.6+

### Run

```bash
git clone https://github.com/Sakterisk/event-and-monte-carlo-simulation.git
cd event-and-monte-carlo-simulation
mvn javafx:run
```

### Build JAR

```bash
mvn package
java -jar target/semestralka-dis-1.0-SNAPSHOT.jar
```

## What I Learned

- Designing a simulation kernel from first principles (no external simulation libraries)
- Implementing and validating custom probability distributions using statistical tests
- Applying online algorithms to keep memory usage O(1) regardless of simulation length
- Connecting a live JavaFX UI to a background simulation thread safely
- Structuring a multi-module Java/Maven project with clear separation of concerns

## Course

Discrete Simulation (DIS) — Faculty of Management Science and Informatics, University of Žilina
