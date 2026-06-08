package sk.uniza.adamec2.sem_two.terminal.entity.factory;

import sk.uniza.adamec2.generator.Gen;
import sk.uniza.adamec2.sem_two.entity.factory.EntityFactory;
import sk.uniza.adamec2.sem_two.terminal.entity.Person;

public class PersonFactory implements EntityFactory<Person> {

    private final Gen<Integer> baggageGen;

    public PersonFactory(Gen<Integer> baggageGen) {
        this.baggageGen = baggageGen;
    }

    @Override
    public Person createEntity(int id , double arrivalTime) {
        return new Person(id, baggageGen.next(),  arrivalTime);
    }
}
