package persistance;

import domain.Car;
import domain.Rental;
import domain.Station;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.Collection;

public class JPARentalRepository {
    public JPARentalRepository(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManagerFactory entityManagerFactory;


    public Rental finish(Rental rental, Station station, double drivenKm){

        if(station == null)
            throw new IllegalArgumentException();
        if(drivenKm == Double.POSITIVE_INFINITY || Double.isNaN(drivenKm) || drivenKm <= 0)
            throw new IllegalArgumentException();
        if(rental.getEnd() != null)
            throw new IllegalArgumentException();

        var entityManager = entityManagerFactory.createEntityManager();

        rental.setEnd(LocalDateTime.now());
        rental.setDrivenKm(drivenKm);
        rental.setReturnStation(station);

        var car = rental.getCar();
        car.setMileage(car.getMileage()+drivenKm);
        car.setLocation(station);

        try{
            entityManager.getTransaction().begin();
            entityManager.merge(rental);
            entityManager.merge(car);
            entityManager.getTransaction().commit();
        }
        catch (Exception e){
            entityManager.getTransaction().rollback();
            throw e;
        }
        finally {
            entityManager.close();
        }
        return rental;
    }

    public boolean checkIfCarIsAvaliable(Car car, LocalDateTime start, LocalDateTime end){
        var entityManager = entityManagerFactory.createEntityManager();
        try{
            String jqpl = """
                    Select rental from Rental rental
                    where rental.car = :car and
                    :start <= rental.end and
                    rental.beginning >= :end
                    """;
            var query = entityManager.createQuery(jqpl,Rental.class)
                    .setParameter("car",car)
                    .setParameter("start",start)
                    .setParameter("end",end);
            return !query.getResultList().isEmpty();
        }
        finally {
            entityManager.close();
        }
    }

}
