package sk.uniza.adamec2.sem_two.entity;

public abstract class FrontableEntity extends Entity {

    private double frontArrivalTime = 0.0;

    public FrontableEntity(int id) {
        super(id);
    }

    public double getFrontArrivalTime() {
        return frontArrivalTime;
    }

    public void setFrontArrivalTime(double arrivalTime) {
        this.frontArrivalTime = arrivalTime;
    }

}
