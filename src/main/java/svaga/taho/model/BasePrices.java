package svaga.taho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "base_prices")
public class BasePrices {
    @Id
    private Long id;
    @Column(name = "place_price")
    private BigDecimal placePrice; //За посадку
    @Column(name = "kilometre_price")
    private BigDecimal kilometrePrice; //За 1 километр
    @Column(name = "low_price")
    private BigDecimal lowPrice;
    @Column(name = "mid_price")
    private BigDecimal midPrice;
    @Column(name = "high_price")
    private BigDecimal highPrice;
}
