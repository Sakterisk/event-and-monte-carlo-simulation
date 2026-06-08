package sk.uniza.adamec2.sem_two.terminal.segment;

import sk.uniza.adamec2.generator.ContinuousUniformGen;
import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;
import sk.uniza.adamec2.sem_two.terminal.DetectorGen;
import sk.uniza.adamec2.sem_two.terminal.TerminalSimulation;
import sk.uniza.adamec2.sem_two.terminal.entity.Crate;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;
import sk.uniza.adamec2.sem_two.terminal.event.DetectorEvent;
import sk.uniza.adamec2.sem_two.terminal.event.factory.DetectorEventFactory;
import sk.uniza.adamec2.sem_two.terminal.event.XrayEvent;
import sk.uniza.adamec2.sem_two.terminal.event.factory.PlaceCrateEventFactory;
import sk.uniza.adamec2.sem_two.terminal.event.factory.TakeCrateEventFactory;
import sk.uniza.adamec2.sem_two.terminal.event.factory.XrayEventFactory;

public class SingleTerminal {

    private final TerminalSimulation sim;

    private final Front<Person> frontBeforePlaceCrate;
    private final Front<Person> frontBeforeTakeCrate;
    private final Front<Crate> frontAfterXray;
    private final ServiceFront<DetectorEvent, Person> detectorService;
    private final ServiceFront<XrayEvent, Crate> xrayService;

    private final PlaceCrateEventFactory placeCrateFactory;
    private final TakeCrateEventFactory takeCrateFactory;

    public SingleTerminal(TerminalSimulation sim) {

        this.sim = sim;

        // ---- Fronts initialization
        frontBeforePlaceCrate = new Front<>(sim);
        frontBeforeTakeCrate = new Front<>(sim);
        frontAfterXray = new Front<>(sim, 5);

        // ---- Detector service initialization
        Front<Person> detectorFront = new Front<>(sim);
        Gen<Double> detectorGen = new DetectorGen();
        detectorService = new ServiceFront<>(detectorFront, detectorGen, sim);
        DetectorEventFactory decFactory = new DetectorEventFactory(detectorService, this);
        detectorService.setEventFactory(decFactory);

        // ---- X-ray service initialization
        Front<Crate> xrayFront = new Front<>(sim, 4);
        Gen<Double> xrayGen = new ContinuousUniformGen(9.0 / 3600.0, 46.0 / 3600.0);
        xrayService = new ServiceFront<>(xrayFront, xrayGen, sim);
        XrayEventFactory xrayFactory = new XrayEventFactory(xrayService, this);
        xrayService.setEventFactory(xrayFactory);

        // ---- Factories initialization
        placeCrateFactory = new PlaceCrateEventFactory(frontBeforePlaceCrate, this);
        takeCrateFactory = new TakeCrateEventFactory(frontBeforeTakeCrate, frontAfterXray, this);
    }

    public void arrivalToTerminal(Person person) {
        sim.addEvent(placeCrateFactory.createEvent(sim.getTime(), sim, person));
    }

    public void placeCrateBeforeXray(Crate crate) {
        xrayService.planNewEntityArrival(crate);
    }

    public void tryPlanPlaceCrateEvent() {
        sim.addEvent(placeCrateFactory.createEvent(sim.getTime(), sim, null));
    }

    public void tryPlanTakeCrateEvent() {
        sim.addEvent(takeCrateFactory.createEvent(sim.getTime(), sim, null));
    }

    public void personTransitionToTakeCrate(Person person) {
        sim.addEvent(takeCrateFactory.createEvent(sim.getTime(), sim, person));
    }

    public void personTransitionToDetector(Person person) {
        detectorService.planNewEntityArrival(person);
    }

    public void tryStartXrayServiceEvent() {
        xrayService.tryToStartService();
    }

    public int freeSpaceBeforeXray() {
        return xrayService.getFreeSpaceFront();
    }

    public int getQueueLengthBeforePlaceCrate() {
        return frontBeforePlaceCrate.size();
    }

    public int getQueueLengthBeforeTakeCrate() {
        return frontBeforeTakeCrate.size();
    }

    public boolean isAfterXrayFull() {
        return frontAfterXray.freeSpace() == 0;
    }

    public void addCrateToFrontAfterXray(Crate crate) {
        frontAfterXray.add(crate);
    }

    public void personExit(Person person) {
        sim.personLeftTerminal(person);
    }

    // ---- Read-only accessors for GUI ----

    public Front<Person> getFrontBeforePlaceCrateSegment() {
        return frontBeforePlaceCrate;
    }

    public Front<Person> getFrontBeforeTakeCrateSegment() {
        return frontBeforeTakeCrate;
    }

    public Front<Crate> getFrontAfterXraySegment() {
        return frontAfterXray;
    }

    public ServiceFront<DetectorEvent, Person> getDetectorServiceSegment() {
        return detectorService;
    }

    public ServiceFront<XrayEvent, Crate> getXrayServiceSegment() {
        return xrayService;
    }

    public void clear() {
        frontAfterXray.clear();
        frontBeforePlaceCrate.clear();
        frontBeforeTakeCrate.clear();
        xrayService.clear();
        detectorService.clear();
    }

    public void changeFrontBeforeXrayMaxSize(int maxSize) {
        xrayService.changeFrontMaxSize(maxSize);
    }

    public void changeFrontAfterXrayMaxSize(int maxSize) {
        frontAfterXray.changeMaxSize(maxSize);
    }
}
