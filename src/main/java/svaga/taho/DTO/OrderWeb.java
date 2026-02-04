package svaga.taho.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderWeb {
    private String orderId;
    private BigDecimal price;
    private String startAddress;
    private String endAddress;
    private LocalDateTime orderTime;
}
