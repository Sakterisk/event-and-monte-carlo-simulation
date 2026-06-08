package sk.uniza.adamec2.sem_two.terminal.entity;

import sk.uniza.adamec2.sem_two.entity.FrontableEntity;
import sk.uniza.adamec2.util.TimeParser;

import java.util.LinkedList;

public class Person extends FrontableEntity {

    private final double arriveTime;
    private final int numberOfBaggage;
    private final LinkedList<Baggage> baggage;

    public Person(int id, int numberOfBaggage, double arriveTime) {
        super(id);
        this.arriveTime = arriveTime;
        this.numberOfBaggage = numberOfBaggage;
        this.baggage = new LinkedList<>();
        for (int i = 1; i <= numberOfBaggage; i++) {
            baggage.add(new Baggage(i, this.id));
        }
    }

    public Crate putBaggageInCrate() {
        if (baggage.isEmpty()) {
            return null;
        }
        Baggage bag = baggage.poll();
        return new Crate(bag);
    }

    public void takeBaggageFromCrate(Crate crate) {
        if (crate == null || crate.getBaggage() == null) {
            throw new IllegalArgumentException("Crate or baggage cannot be null.");
        }
        Baggage bag = crate.getBaggage();
        if (!bag.belongsToOwner(this)) {
            System.out.println("Warning: Baggage from crate does not belong to owner. Person ID: " + this.id + ", Baggage owner ID: " + bag.getOwnerID());
            throw new IllegalArgumentException("Baggage from crate does not belong to owner. Person ID: " + this.id + ", Baggage owner ID: " + bag.getOwnerID());
        }
        baggage.add(bag);
    }

    public int getNumberOfBaggage() {
        return numberOfBaggage;
    }

    public int getNumberOfHoldingBaggage() {
        return baggage.size();
    }

    public double getArriveTime() {
        return arriveTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Person {\n");
        sb.append("\tid=").append(id).append("\n");
        sb.append("\tarriveTime=").append(TimeParser.parseTime(arriveTime)).append("\n");
        sb.append("\tnumberOfBaggage=").append(numberOfBaggage).append("\n");
        sb.append("\tbaggage=[\n");
        if  (baggage.isEmpty()) {
            sb.append("\t\tNo baggage\n");
            sb.append("\t]\n");
        } else {
            for (Baggage bag : baggage) {
                sb.append("\t\t").append(bag).append("\n");
            }
            sb.append("\t]\n");
        }
        sb.append("}");
        return sb.toString();

    }

    public String asLabelWithBaggage() {
        // baggage list contains Baggage objects whose id already encodes owner + index (ownerID*100+idLocal)[cite:30]
        if (baggage.isEmpty()) {
            return "P" + id + " - {}";
        }
        String bags = baggage.stream()
                .map(b -> "B" + b.getId())
                .collect(java.util.stream.Collectors.joining(", "));
        return "P" + id + " - {" + bags + "}";
    }
}
