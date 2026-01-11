package svaga.taho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import svaga.taho.model.BasePrices;


@Repository
public interface IBasePricesRepository extends JpaRepository<BasePrices, String> {
    
}
