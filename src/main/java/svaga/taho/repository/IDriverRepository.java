package svaga.taho.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import svaga.taho.model.Driver;
import svaga.taho.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

@Repository
public interface IDriverRepository extends JpaRepository<Driver, String> {
    List<Driver> findByStatus(DriverStatus status);
    List<Driver> findByStatusIn(List<DriverStatus> statuses); //поиск по нескольким статусам сразу
    List<Driver> findTop5ByStatus(DriverStatus status);
}
