package sk.uniza.adamec2.gui;

import sk.uniza.adamec2.core.EventCore;

/**
 * Callback interface that the EventSim core calls whenever the GUI
 * should be refreshed. Decouples the simulation from JavaFX.
 */
@FunctionalInterface
public interface SimulationObserver {
    void refresh(EventCore sim);
}
