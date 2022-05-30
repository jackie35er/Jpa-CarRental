package persistence;

import domain.*;
import lombok.experimental.UtilityClass;
import service.JpaService;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class Fixtures {

    public List<Station> stations;
    public List<Rental> rentals;
    public List<Car> cars;

    public void save(EntityManagerFactory factory) {
        var service = new JpaService(factory);

        stations = Stream.of(
                        new Station(null, "Wien Nord"),
                        new Station(null, "Wien Mitte"),
                        new Station(null, "St. Pölten")
                )
                .map(service::save)
                .toList();

        cars = Stream.of(
                        new Car("W-123ER", 123, "X1", stations.get(0)),
                        new Car("P-VN3X", 0, "Model X", stations.get(0)),
                        new Car("KS-SHV234", 1_234, "C4", stations.get(1)),
                        new Car("W-456UI", 10_234, "Passat", null)
                )
                .map(service::save)
                .toList();

        rentals = Stream.of(
                        Rental.builder()
                                .beginning(LocalDateTime.of(2021, 12, 31, 12, 30))
                                .end(LocalDateTime.of(2022, 1, 2, 10, 0))
                                .car(cars.get(0))
                                .rentalStation(stations.get(0))
                                .returnStation(stations.get(0))
                                .drivenKm(2_000.0)
                                .build(),
                        Rental.builder()
                                .beginning(LocalDateTime.of(2021, 8, 1, 0, 0))
                                .end(LocalDateTime.of(2021, 8, 1, 10, 0))
                                .car(cars.get(1))
                                .rentalStation(stations.get(0))
                                .returnStation(stations.get(1))
                                .drivenKm(400.0)
                                .build(),
                        Rental.builder()
                                .beginning(LocalDateTime.of(2022, 1, 3, 0, 0))
                                .car(cars.get(0))
                                .rentalStation(stations.get(0))
                                .build()
                )
                .map(service::save)
                .toList();
    }
}
