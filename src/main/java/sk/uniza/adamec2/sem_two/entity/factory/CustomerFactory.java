package sk.uniza.adamec2.sem_two.entity.factory;

import sk.uniza.adamec2.sem_two.entity.Customer;

public class CustomerFactory implements EntityFactory<Customer> {

    @Override
    public Customer createEntity(int id, double arrivalTime) {
        return new Customer(id, arrivalTime);
    }
}
