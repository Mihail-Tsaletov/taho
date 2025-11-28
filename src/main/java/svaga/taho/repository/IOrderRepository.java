package svaga.taho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import svaga.taho.model.Order;
import svaga.taho.model.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOrderRepository extends JpaRepository<Order, String> {

    // Для менеджера: все заказы с определённым статусом
    List<Order> findByStatus(OrderStatus status);

    //Поиск по айди
    Optional<Order> findByOrderId(String Id);

    // Для клиента: все заказы по clientId
    List<Order> findByClientId(String clientId);

    // Для водителя: все заказы по driverId
    List<Order> findByDriverId(String driverId);

    // Комбинированный поиск (например, PENDING + ASSIGNED)
    List<Order> findByStatusIn(List<OrderStatus> statuses);

    //Поиск заказов от конкретного пользователя с несколькими статусами(PENDING, ASSIGNED, ACCEPTED)
    List<Order> findByClientIdAndStatusIn(String clientId, List<OrderStatus> statuses);

    List<Order> findByDriverIdAndStatusIn(String driverId, List<OrderStatus> statuses);
}
