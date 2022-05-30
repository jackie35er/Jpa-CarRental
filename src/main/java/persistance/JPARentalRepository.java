package persistance;

import domain.Car;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class JPARentalRepository {
    public JPARentalRepository(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManagerFactory entityManagerFactory;


}
