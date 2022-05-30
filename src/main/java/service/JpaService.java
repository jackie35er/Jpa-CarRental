package service;

import domain.*;

import jakarta.persistence.EntityManagerFactory;
import persistance.JPACarRepository;
import persistance.JPAGenericRepository;
import persistance.JPARentalRepository;
import persistance.JPAStationRepository;

import java.util.*;

public class JpaService implements Service {

    public JpaService(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
        this.jpaCarRepository = new JPACarRepository(entityManagerFactory);
        this.jpaRentalRepository = new JPARentalRepository(entityManagerFactory);
        this.jpaStationRepository = new JPAStationRepository(entityManagerFactory);
        this.jpaGenericRepository = new JPAGenericRepository(entityManagerFactory);
    }

    private EntityManagerFactory entityManagerFactory;

    private JPACarRepository jpaCarRepository;
    private JPARentalRepository jpaRentalRepository;
    private JPAStationRepository jpaStationRepository;
    private JPAGenericRepository jpaGenericRepository;

    @Override
    public Rental save(Rental rental) {
        if (jpaRentalRepository.checkIfCarIsAvaliable(rental.getCar(),rental.getBeginning(),rental.getEnd()))
            throw new IllegalArgumentException();
        return jpaGenericRepository.safe(rental);
    }

    @Override
    public Station save(Station station) {
        return jpaGenericRepository.safe(station);
    }

    @Override
    public Car save(Car car) {
        return jpaGenericRepository.safe(car);
    }

    @Override
    public List<Station> findAllStations() {
        return new ArrayList<>(jpaGenericRepository.findAll(Station.class));
    }

    @Override
    public List<Car> findAllCars() {
        return new ArrayList<>(jpaGenericRepository.findAll(Car.class));
    }

    @Override
    public List<Rental> findAllRentals() {
        return new ArrayList<>(jpaGenericRepository.findAll(Rental.class));
    }

    @Override
    public Optional<Rental> findRentalById(long id) {
        return jpaGenericRepository.findByID(id,Rental.class);
    }

    @Override
    public Set<Car> findCarsStationedAt(Station station) {
        return new HashSet<>(jpaCarRepository.getCarsAtStation(station));
    }

    @Override
    public Rental finish(Rental rental, Station station, double drivenKm) {
        return jpaRentalRepository.finish(rental,station,drivenKm);
    }
}