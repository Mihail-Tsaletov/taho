package svaga.taho.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @Column(name = "order_id", length = 50, nullable = false, insertable = true, updatable = false)
    private String orderId;

    @Column(name = "client_id", length = 50)
    private String clientId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "manager_id", length = 50)
    private String managerId;

    private String startPoint;
    private String endPoint;

    @JsonProperty("startAddress")
    @Column(name = "start_address")
    private String startAddress;

    @JsonProperty("endAddress")
    @Column(name = "end_address")
    private String endAddress;

    private BigDecimal price;

    private boolean inCity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private LocalDateTime orderTime;
    private LocalDateTime assignedTime;
    private LocalDateTime acceptanceTime;
    private LocalDateTime pickupTime;
    private LocalDateTime dropOffTime;

    @PrePersist
    public void generateId() {
        if (this.orderId == null) {
            this.orderId = java.util.UUID.randomUUID().toString();
        }
    }
}
