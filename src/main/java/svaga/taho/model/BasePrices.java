package svaga.taho.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table("base_prices")
public class BasePrices {
    private BigDecimal placePrice; //За посадку
    private BigDecimal kilometrePrice; //За 1 километр
}
