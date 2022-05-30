package domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class Car {

    @Id
    @Size(min = 4, max = 9)
    private String plate;

    @Min(0)
    private double mileage;

    private String model;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Car car = (Car) o;
        return plate != null && Objects.equals(plate, car.plate);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @ManyToOne(cascade = {CascadeType.MERGE})
    private Station location;


}
