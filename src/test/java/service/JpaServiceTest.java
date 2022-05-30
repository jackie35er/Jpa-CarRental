package service;

import domain.Car;
import domain.Rental;
import domain.exceptions.CarNotAvailableException;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import persistence.Fixtures;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpaServiceTest {

    private static final String PERSISTENCE_UNIT_NAME = "car-rental";

    private EntityManagerFactory factory;

    @BeforeAll
    private static void setLogLevel() {
        Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
    }

    @BeforeEach
    void setupDatabase() {
        factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        Fixtures.save(factory);
    }

    @AfterEach
    void closeFactory() {
        factory.close();
    }

    @Test
    void finds_all_stations() {
        var service = new JpaService(factory);

        assertThat(service.findAllStations())
                .containsExactlyInAnyOrderElementsOf(Fixtures.stations);
    }

    @Test
    void finds_all_cars() {
        var service = new JpaService(factory);

        assertThat(service.findAllCars())
                .containsExactlyInAnyOrderElementsOf(Fixtures.cars);
    }

    @Test
    void finds_all_rentals() {
        var service = new JpaService(factory);

        assertThat(service.findAllRentals())
                .containsExactlyInAnyOrderElementsOf(Fixtures.rentals);
    }

    @Test
    void finds_cars_at_station() {
        var service = new JpaService(factory);

        assertThat(service.findCarsStationedAt(Fixtures.stations.get(0)))
                .containsExactlyInAnyOrderElementsOf(Fixtures.cars.subList(0, 2));
    }

    @Nested
    class FindingRentalById {

        @Test
        void loads_related_objects_for_finished_rental() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .drivenKm(1_000.0)
                    .beginning(LocalDateTime.of(2018, 1, 1, 0, 0))
                    .end(LocalDateTime.of(2018, 1, 2, 0, 0))
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .car(Fixtures.cars.get(2))
                    .build();
            var savedRental = service.save(rental);

            var queried = service.findRentalById(savedRental.getId());

            assertThat(queried)
                    .get()
                    .isEqualTo(savedRental)
                    .hasNoNullFieldsOrProperties();
        }

        @Test
        void loads_related_objects_for_unfinished_rental() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2022, 1, 1, 0, 0))
                    .rentalStation(Fixtures.stations.get(0))
                    .car(Fixtures.cars.get(2))
                    .build();
            var savedRental = service.save(rental);

            var queried = service.findRentalById(savedRental.getId());

            assertThat(queried)
                    .get()
                    .isEqualTo(savedRental)
                    .hasNoNullFieldsOrPropertiesExcept("returnStation", "end", "drivenKm");
        }

        @Test
        void returns_empty_for_unknown_id() {
            var service = new JpaService(factory);

            var queried = service.findRentalById(404);

            assertThat(queried)
                    .isEmpty();
        }
    }

    @Nested
    class SavingRental {

        private final static Car carWithoutRentals = Fixtures.cars.get(2);

        @Test
        void works_for_finished_rentals() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2018, 1, 1, 0, 0))
                    .end(LocalDateTime.of(2018, 1, 2, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(1))
                    .returnStation(Fixtures.stations.get(2))
                    .drivenKm(1_000.0)
                    .build();

            var saved = service.save(rental);

            assertThat(saved)
                    .extracting(Rental::getId)
                    .isNotNull();
            var queried = service.findRentalById(saved.getId());
            assertThat(queried)
                    .get()
                    .isEqualTo(saved);
        }

        @Test
        void works_for_ongoing_rentals() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2018, 1, 1, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(1))
                    .build();

            var saved = service.save(rental);

            assertThat(saved)
                    .extracting(Rental::getId)
                    .isNotNull();
            var queried = service.findRentalById(saved.getId());
            assertThat(queried)
                    .get()
                    .isEqualTo(saved);
        }

        @Test
        void fails_if_end_before_begin() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2022, 1, 2, 0, 0))
                    .end(LocalDateTime.of(2022, 1, 1, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();

            assertThatThrownBy(() -> service.save(rental));
        }

        @Test
        void fails_if_finished_and_not_all_fields_set() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2018, 1, 1, 0, 0))
                    .end(null)
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(1))
                    .returnStation(Fixtures.stations.get(1))
                    .drivenKm(null)
                    .build();

            assertThatThrownBy(() -> service.save(rental));
        }

        @Test
        void fails_if_car_already_rented() {
            var service = new JpaService(factory);
            LocalDateTime time = LocalDateTime.of(2020, 1, 1, 0, 0);
            var existingRental = Rental.builder()
                    .beginning(time)
                    .end(time.plusDays(2))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();
            service.save(existingRental);
            var invalidRental = Rental.builder()
                    .beginning(time.plusDays(1))
                    .end(time.plusDays(3))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();

            assertThatThrownBy(() -> service.save(invalidRental))
                    .isInstanceOf(CarNotAvailableException.class);
        }

        @Test
        void fails_if_car_reserved() {
            var service = new JpaService(factory);
            LocalDateTime time = LocalDateTime.of(2020, 1, 1, 0, 0);
            var existingRental = Rental.builder()
                    .beginning(time.plusDays(1))
                    .end(time.plusDays(3))
                    .car(Fixtures.cars.get(2))
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();
            service.save(existingRental);
            var invalidRental = Rental.builder()
                    .beginning(time)
                    .end(time.plusDays(2))
                    .car(Fixtures.cars.get(2))
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();

            assertThatThrownBy(() -> service.save(invalidRental))
                    .isInstanceOf(CarNotAvailableException.class);
        }

        @Test
        void fails_if_car_not_completely_available() {
            var service = new JpaService(factory);
            LocalDateTime time = LocalDateTime.of(2020, 1, 1, 0, 0);
            var existingRental = Rental.builder()
                    .beginning(time.plusDays(1))
                    .end(time.plusDays(3))
                    .car(Fixtures.cars.get(2))
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();
            service.save(existingRental);
            var invalidRental = Rental.builder()
                    .beginning(time)
                    .end(time.plusDays(4))
                    .car(Fixtures.cars.get(2))
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();

            assertThatThrownBy(() -> service.save(invalidRental))
                    .isInstanceOf(CarNotAvailableException.class);
        }

        @Test
        void fails_if_car_in_ongoing_rental() {
            var service = new JpaService(factory);
            LocalDateTime time = LocalDateTime.of(2020, 1, 1, 0, 0);
            var existingRental = Rental.builder()
                    .beginning(time)
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .build();
            service.save(existingRental);
            var invalidRental = Rental.builder()
                    .beginning(time.plusDays(1))
                    .end(time.plusDays(3))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .returnStation(Fixtures.stations.get(0))
                    .drivenKm(10.0)
                    .build();

            assertThatThrownBy(() -> service.save(invalidRental))
                    .isInstanceOf(CarNotAvailableException.class);
        }
    }

    @Nested
    class FinishingRentals {

        private final static Car carWithoutRentals = Fixtures.cars.get(2);

        @Test
        void works() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2020, 1, 1, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .build();
            var unfinishedRental = service.save(rental);

            var finishedRental = service.finish(unfinishedRental, Fixtures.stations.get(1), 12.1);

            assertThat(finishedRental)
                    .hasNoNullFieldsOrProperties();
        }

        @Test
        void fails_for_finished_rentals() {
            var service = new JpaService(factory);
            var returnStation = Fixtures.stations.get(2);
            var drivenKm = 1_000.0;
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2018, 1, 1, 0, 0))
                    .end(LocalDateTime.of(2018, 1, 2, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(1))
                    .returnStation(returnStation)
                    .drivenKm(drivenKm)
                    .build();
            var saved = service.save(rental);

            assertThatThrownBy(() ->
                    service.finish(saved, returnStation, drivenKm))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void fails_without_station() {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2020, 1, 1, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .build();
            var saved = service.save(rental);

            assertThatThrownBy(() ->
                    service.finish(saved, null, 1));
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1.0, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
        void fails_for_invalid_distance(double drivenKm) {
            var service = new JpaService(factory);
            var rental = Rental.builder()
                    .beginning(LocalDateTime.of(2020, 1, 1, 0, 0))
                    .car(carWithoutRentals)
                    .rentalStation(Fixtures.stations.get(0))
                    .build();
            var saved = service.save(rental);

            assertThatThrownBy(() ->
                    service.finish(saved, Fixtures.stations.get(0), drivenKm));
        }

        @Nested
        class Updates {

            @Test
            void car_mileage() {
                var service = new JpaService(factory);
                var car = service.save(
                        new Car("W-NEW201", 10, "Model T", Fixtures.stations.get(0))
                );
                var rental = Rental.builder()
                        .beginning(LocalDateTime.of(2020, 1, 1, 0, 0))
                        .car(car)
                        .rentalStation(Fixtures.stations.get(0))
                        .build();
                var unfinishedRental = service.save(rental);

                var finishedRental = service.finish(unfinishedRental, Fixtures.stations.get(1), 12.1);

                assertThat(finishedRental.getCar())
                        .extracting(Car::getMileage)
                        .asInstanceOf(InstanceOfAssertFactories.DOUBLE)
                        .isCloseTo(22.1, Offset.offset(1e-7));
            }

            @Test
            void car_station() {
                var service = new JpaService(factory);
                var car = service.save(
                        new Car("W-NEW201", 10, "Model T", Fixtures.stations.get(0))
                );
                var rental = Rental.builder()
                        .beginning(LocalDateTime.of(2020, 1, 1, 0, 0))
                        .car(car)
                        .rentalStation(Fixtures.stations.get(0))
                        .build();
                var unfinishedRental = service.save(rental);
                var returnStation = Fixtures.stations.get(1);

                var finishedRental = service.finish(unfinishedRental, returnStation, 12.1);

                assertThat(finishedRental.getCar())
                        .extracting(Car::getLocation)
                        .isEqualTo(returnStation);
            }
        }
    }
}