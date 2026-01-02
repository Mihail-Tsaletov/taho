package svaga.taho.service;

import jakarta.transaction.Transactional;
import lombok.val;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import svaga.taho.model.*;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IOrderRepository;
import svaga.taho.repository.IUserRepository;

import java.time.LocalDateTime;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IDriverRepository driverRepository;
    @Autowired
    private SseService sseService;

    @Transactional
    public String createOrder(Order order, String uid) {
        try {
            //String uid = SecurityContextHolder.getContext().getAuthentication().getName(); Пока коммент, передаем uid как аргумент
            if (uid == null) {
                log.error("User authentication not found");
                throw new IllegalStateException("User must be authenticated");
            }

            //Проверка существования юзера
            User user = userRepository.findById(uid).orElseThrow(() -> {
                log.error("User {} does not exist", uid);
                return new IllegalStateException("User not found");
            });

            //Валидация заказа
            if (order.getStartPoint() == null || order.getStartPoint().isEmpty() || order.getEndPoint() == null || order.getEndPoint().isEmpty()) {
                log.error("Dont have a start point or end point");
                throw new IllegalArgumentException("Dont have a start point or end point");
            }

            //Создание заказа
            order.setClientId(uid);
            order.setStatus(OrderStatus.PENDING);
            order.setOrderTime(LocalDateTime.now());
            order.setDriverId(null);
            order.setAcceptanceTime(null);
            order.setAssignedTime(null);
            order.setPickupTime(null);
            order.setDropOffTime(null);

            orderRepository.save(order);

            log.info("Order created with id {}", order.getOrderId());
            return order.getOrderId();
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw e;
        }
    }
    @Transactional
    public void acceptOrder(String orderId, String uid) {
        try {
            if (uid == null) {
                log.error("Driver authentication not found");
                throw new IllegalStateException("Driver must be authenticated");
            }

            //Проверка водителя, поиск по uid тк токен показывает юзерский айди
            Driver driver = driverRepository.findByUserId(uid).orElseThrow(() -> {
                log.error("Driver {} not found", uid);
                return new IllegalStateException("Driver not found");
            });

            if (!DriverStatus.ASSIGNED.equals(driver.getStatus())) {
                log.error("Driver {} is not ASSIGNED, current status: {}", uid, driver.getStatus());
                throw new IllegalStateException("Driver is not ASSIGNED");
            }

            //Проверка заказа
            Order order = orderRepository.findByOrderId(orderId).orElseThrow(() -> {
                log.error("Order {} does not exist", orderId);
                return new IllegalArgumentException("Order not found");
            });


            if (!OrderStatus.PENDING.equals(order.getStatus()) && !OrderStatus.ASSIGNED.equals(order.getStatus())) {
                log.error("Order {} is not ASSIGNED or PENDING, current status: {}", orderId, order.getStatus());
                throw new IllegalStateException("Order cannot be accepted");
            }

            //Обновление статуса заказа
            order.setDriverId(driver.getDriverId());
            order.setStatus(OrderStatus.ACCEPTED);
            order.setAcceptanceTime(LocalDateTime.now());

            //Обновление статуса водителя
            driver.setStatus(DriverStatus.BUSY);

            driverRepository.save(driver);
            orderRepository.save(order);

            // ← ОТПРАВЛЯЕМ ОБНОВЛЕНИЕ ВСЕМ ПОДПИСЧИКАМ
            sseService.sendOrderUpdate(orderId, Map.of(
                    "status", order.getStatus(),
                    "driverName", driver.getName(),
                    "driverPhone", driver.getPhoneNumber()
            ));

            log.info("Order {} accepted by driver {}", orderId, driver.getDriverId());
        } catch (Exception e) {
            log.error("Failed to accept order: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.error("Order {} does not found", orderId);
                    return new IllegalArgumentException("Order not found");
                });

        if (!Arrays.toString(OrderStatus.values()).contains(order.getStatus().toString())) {
            log.error("Invalid status {}", status);
            throw new IllegalArgumentException("Invalid status");
        }

        //Обновление времени
        if (OrderStatus.ACCEPTED.equals(status)) {
            order.setAcceptanceTime(LocalDateTime.now());
        } else if (OrderStatus.PICKED_UP.equals(status)) {
            order.setPickupTime(LocalDateTime.now());
        } else if (OrderStatus.COMPLETED.equals(status)) {
            order.setDropOffTime(LocalDateTime.now());
        }

        order.setStatus(status);
        orderRepository.save(order);

        // ← ОТПРАВЛЯЕМ ОБНОВЛЕНИЕ ВСЕМ ПОДПИСЧИКАМ
        sseService.sendOrderUpdate(orderId, Map.of(
                "status", status
        ));
        log.info("Order status updated with id {} SSE SEND", order.getOrderId());
    }
    @Transactional
    public Order getCurrentOrder(String orderId) {
        try {
            return orderRepository.findById(orderId).orElseThrow(() -> {
                log.error("Order {} does not found", orderId);
                return new IllegalArgumentException("Order not found");
            });
        } catch (Exception e) {
            log.error("Failed to get order: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public List<Order> getOrdersWithStatus(String status) {
        try {
            if (!Arrays.toString(OrderStatus.values()).contains(status)) {
                log.error("Invalid status {}", status);
                throw new IllegalArgumentException("Invalid status");
            }

            return orderRepository.findByStatus(OrderStatus.valueOf(status));
        } catch (Exception e) {
            log.error("Failed to get active orders: {}", e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void putDriverInOrder(String driverId, String orderId) {
        try {
            //Проверка на существование заказа
            Order order = orderRepository.findByOrderId(orderId).orElseThrow(() -> {
                log.error("Order {} does not found", orderId);
                return new IllegalArgumentException("Order not found");
            });

            //Проверка на существование водителя и его статус
            Driver driver = driverRepository.findById(driverId).orElseThrow(() -> {
                log.error("Driver {} not found", driverId);
                return new IllegalStateException("Driver not found");
            });
            if (!driver.getStatus().equals(DriverStatus.AVAILABLE)) {
                log.error("Driver {} status is not AVAILABLE, status: {}", driverId, driver.getStatus());
                throw new IllegalArgumentException("Driver has another status");
            }

            // Меняем статус водителя
            driver.setStatus(DriverStatus.ASSIGNED);

            // Обновляем заказ
            order.setStatus(OrderStatus.ASSIGNED);
            order.setAssignedTime(LocalDateTime.now());
            order.setDriverId(driver.getDriverId());

            Map<String, Object> data = Map.of(
                    "status", OrderStatus.ASSIGNED,
                    "id", order.getOrderId(),
                    "startPoint", order.getStartPoint(),
                    "endPoint", order.getEndPoint(),
                    "startAddress", order.getStartAddress(),
                    "endAddress", order.getEndAddress(),
                    "passengerName", getClientName(order.getClientId()),
                    "passengerPhone", getClientPhone(order.getClientId()),
                    "distance", "2.4 км",
                    "price", "200"
            );

            driverRepository.save(driver);
            orderRepository.save(order);
            sseService.notifyDriverAboutOrder(driverId, data);


            log.info("Driver {} successfully assigned to order {}", driverId, orderId);
        } catch (Exception e) {
            log.error("Failed to put driver {} in order {} error: {}", driverId, orderId, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public List<Order> getOrdersByUIDAndStatuses(String uid, List<OrderStatus> statuses) {
        User user = userRepository.findById(uid).orElseThrow(() -> {
            log.error("User {} does not exist", uid);
            return new IllegalStateException("User not found");
        });

        try {
            List<Order>  byClientIdAndStatusIn = orderRepository.findByClientIdAndStatusIn(uid, statuses);
            log.info("Orders by client id {} and status {} in {}",uid, statuses, byClientIdAndStatusIn);
            return byClientIdAndStatusIn;
        } catch (Exception e) {
            log.error("Failed to get orders by user: {} with statuses: {}. Error: {}", uid, statuses.toString(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public List<Order> getOrdersByDriverIdAndStatuses(String driverId, List<OrderStatus> statuses) {
        try {
            List<Order> byDriverIdAndStatusIn = orderRepository.findByDriverIdAndStatusIn(driverId, statuses);
            log.info("Orders by driver id {} and status {} in {}",driverId, statuses, byDriverIdAndStatusIn);
            return byDriverIdAndStatusIn;
        } catch (Exception e) {
            log.error("Failed to get orders by driver: {} with statuses: {}. Error: {}", driverId, statuses.toString(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void orderArrived(String orderId) {
        try{
            Order order = orderRepository.findByOrderId(orderId).orElseThrow(() -> {
                log.error("Order {} not found", orderId);
                return new IllegalStateException("Order not found");
            });
            if (!OrderStatus.ACCEPTED.equals(order.getStatus())) {
                log.error("Bad status of order {}", order.getStatus());
                throw new IllegalStateException("Bad status of order");
            }

            updateOrderStatus(orderId, OrderStatus.COMPLETED);
            Driver driver = driverRepository.findByDriverId(order.getDriverId()).orElseThrow(() -> {
                log.error("Driver {} not found", order.getDriverId());
                return new IllegalStateException("Driver not found");
            });
            driver.setStatus(DriverStatus.AVAILABLE);
            driverRepository.save(driver);
            sseService.sendOrderUpdate(orderId, Map.of(
                    "status", OrderStatus.COMPLETED
            ));
            log.info("Driver {} end order {}", driver.getDriverId(), orderId);
        }catch (Exception e) {
            log.error("Failed to arrived orders with id: {}. Error: {}", orderId, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public String getClientPhone(String clientId) {
        return userRepository.findById(clientId)
                .map(User::getPhone)
                .orElse("Неизвестно");
    }

    @Transactional
    public String getClientName(String clientId) {
        return userRepository.findById(clientId)
                .map(User::getName)
                .orElse("Неизвестно");
    }
}
