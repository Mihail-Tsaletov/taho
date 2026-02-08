package svaga.taho.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Entity
@Table(name = "drivers")
@Data
public class Driver {
    @Id
    @Column(name = "driver_id", length = 50, nullable = false, insertable = true, updatable = false)
    private String driverId;

    @Column(name = "user_id", length = 50, unique = true)
    private String userId;

    private String name;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private DriverStatus status = DriverStatus.PENDING;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "last_district")
    private String lastDistrict;

    @PrePersist
    public void generateId() {
        if (this.driverId == null) {
            this.driverId = java.util.UUID.randomUUID().toString();
        }
    }
}
