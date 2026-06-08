package sk.uniza.adamec2.sem_two.event;

import sk.uniza.adamec2.core.EventCore;
import sk.uniza.adamec2.sem_two.entity.FrontableEntity;
import sk.uniza.adamec2.sem_two.segment.Front;
import sk.uniza.adamec2.sem_two.segment.Service;
import sk.uniza.adamec2.sem_two.segment.ServiceFront;

public abstract class ServiceFrontEvent<SE extends ServiceFrontEvent<SE, E>, E extends FrontableEntity> extends ServiceEvent<SE, E> {

    private final ServiceFront<SE, E> serviceFront;

    public ServiceFrontEvent(double time, EventCore simulation, E entity, Service<SE, E> serviceFront) {
        super(time, simulation, entity, serviceFront);
        this.serviceFront = (ServiceFront<SE, E>) serviceFront;
    }

    @Override
    public void execute() {
        if (entity == null) {
            if (serviceFront.getEntity() != null) {
                E e = serviceFront.getEntity();
                serviceFront.endService();
                afterServiceEnd(e);
            }
            if (!serviceFront.isEmptyFront() && conditionStartService() && serviceFront.getEntity() == null) {
                serviceFront.startService(serviceFront.pollFront());
                afterStartService();
            }
        } else {
            if (serviceFront.isAvailable() && conditionStartService()) {
                serviceFront.startService(entity);
                afterStartService();
                serviceFront.addZeroWaitTimeFront();
                serviceFront.willArriveMinus();
            } else {
                serviceFront.addToFront(entity);
            }
        }
    }

    protected boolean conditionStartService() {
        return true;
    }
    abstract protected void afterStartService();
}
