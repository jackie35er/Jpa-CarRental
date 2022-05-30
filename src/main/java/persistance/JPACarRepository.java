package persistance;

import domain.Car;
import domain.Station;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import javax.swing.tree.ExpandVetoException;
import java.util.Collection;
import java.util.Optional;

public class JPACarRepository {
    public JPACarRepository(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManagerFactory entityManagerFactory;

    public Collection<Car> getCarsAtStation(Station station){
        var entityManager = entityManagerFactory.createEntityManager();

        try{
            String jpql = """
                    Select car from Car car\040
                    where car.location = :station
                    """;
            var query = entityManager.createQuery(jpql,Car.class)
                    .setParameter("station",station);
            return query.getResultList();
        }
        finally {
            entityManager.close();
        }
    }


}
