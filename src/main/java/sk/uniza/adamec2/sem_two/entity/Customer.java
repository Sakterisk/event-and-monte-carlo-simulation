package sk.uniza.adamec2.sem_two.entity;

import sk.uniza.adamec2.util.TimeParser;

public class Customer extends FrontableEntity {

    private double arrivalTime = 0.0;

    public Customer(int id, double arrivalTime) {
        super(id);
        this.arrivalTime = arrivalTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public String toString() {
        return "Customer {\n" + "\tid=" + id + ",\n\tarrivalTime= " + TimeParser.parseTime(arrivalTime) + "\n}";
    }
}
