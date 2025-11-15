package svaga.taho.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.stereotype.Component;

@Entity
@Table(name = "drivers")
@Data
public class Driver {
    @Id
    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "user_id", length = 50, unique = true)
    private String userId;

    private String email;
    private String name;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private DriverStatus status = DriverStatus.PENDING;
}
