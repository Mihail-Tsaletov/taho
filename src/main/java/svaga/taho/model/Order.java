package svaga.taho.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "client_id", length = 50)
    private String clientId;

    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Column(name = "manager_id", length = 50)
    private String managerId;

    private String startPoint;
    private String endPoint;
    private boolean withinCity = true;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private LocalDateTime orderTime;
    private LocalDateTime assignedTime;
    private LocalDateTime acceptanceTime;
    private LocalDateTime pickupTime;
    private LocalDateTime dropOffTime;
}
