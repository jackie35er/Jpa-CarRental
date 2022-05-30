package domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;


import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

@Builder
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Rental {




    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double drivenKm;

    private LocalDateTime beginning;

    @Column(name = "endDate")
    private LocalDateTime end;

    @OneToOne(cascade = {CascadeType.MERGE})
    private Car car;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private Station rentalStation;

    @ManyToOne(cascade = {CascadeType.MERGE})
    private Station returnStation;


    @AssertTrue
    private boolean isEndAfterBeginning(){
        if(end == null)
            return true;
        return end.isAfter(beginning);
    }

    @AssertTrue
    private boolean isAllOrNoneNull(){

        return (drivenKm == null && returnStation == null && end == null) ||
                (drivenKm != null && returnStation != null && end != null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Rental rental = (Rental) o;
        return id != null && Objects.equals(id, rental.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
