package svaga.taho.DTO;

import lombok.Data;

@Data
public class CreateOrderRequest {
    private String startPoint;
    private String endPoint;
}