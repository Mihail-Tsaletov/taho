package svaga.taho.DTO;

import lombok.Data;
import svaga.taho.model.OrderStatus;

@Data
public class DriverOrderResponse {
    String id;
    String startPoint;
    String endPoint;
    String startAddress;
    String endAddress;
    String passengerName;
    String passengerPhone;
    String price;
    OrderStatus status;
    String distance;
}
