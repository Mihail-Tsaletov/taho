package svaga.taho.DTO;

import lombok.Data;
import svaga.taho.model.OrderStatus;

@Data
public class OrderResponse {
    private String startAddress;
    private String endAddress;
    private OrderStatus status;
    private String id;
    private String driverName;
    private String driverPhone;
}