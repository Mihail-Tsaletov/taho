package svaga.taho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import svaga.taho.model.Order;
import svaga.taho.model.OrderStatus;

import java.util.List;

@Repository
public interface IOrderRepository extends JpaRepository<Order, String> {

    // Для менеджера: все заказы с определённым статусом
    List<Order> findByStatus(OrderStatus status);

    // Для клиента: все заказы по clientId
    List<Order> findByClientId(String clientId);

    // Для водителя: все заказы по driverId
    List<Order> findByDriverId(String driverId);

    // Комбинированный поиск (например, NEW + ASSIGNED)
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
