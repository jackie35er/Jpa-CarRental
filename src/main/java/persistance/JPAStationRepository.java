package persistance;

import domain.Car;
import domain.Station;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class JPAStationRepository {

    public JPAStationRepository(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManagerFactory entityManagerFactory;

}
