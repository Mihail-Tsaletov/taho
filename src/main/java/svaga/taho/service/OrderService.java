package svaga.taho.service;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import svaga.taho.model.*;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IOrderRepository;
import svaga.taho.repository.IUserRepository;

import java.time.LocalDateTime;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IDriverRepository driverRepository;

    public String createOrder(Order order, String uid) throws ExecutionException, InterruptedException {
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

    public void acceptOrder(String orderId, String uid) throws ExecutionException, InterruptedException {
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
                log.error("Driver {} is not available, current status: {}", uid, driver.getStatus());
                throw new IllegalStateException("Driver is not available");
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
            log.info("Order {} accepted by driver {}", orderId, driver.getDriverId());
        } catch (Exception e) {
            log.error("Failed to accept order: {}", e.getMessage());
            throw e;
        }
    }

    public void updateOrder(String orderId, String status) throws ExecutionException, InterruptedException {
        try {
            //Проверка заказа
            Order order = orderRepository.findById(orderId).orElseThrow(() -> {
                log.error("Order {} does not found", orderId);
                return new IllegalArgumentException("Order not found");
            });

            if (!Arrays.toString(OrderStatus.values()).contains(order.getStatus().toString())) {
                log.error("Invalid status {}", status);
                throw new IllegalArgumentException("Invalid status");
            }

            //Обновление времени
            if (OrderStatus.ACCEPTED.toString().equals(status)) {
                order.setAcceptanceTime(LocalDateTime.now());
            } else if (OrderStatus.PICKED_UP.toString().equals(status)) {
                order.setPickupTime(LocalDateTime.now());
            } else if (OrderStatus.COMPLETED.toString().equals(status)) {
                order.setDropOffTime(LocalDateTime.now());
            }

            order.setStatus(OrderStatus.valueOf(status));
            orderRepository.save(order);
            log.info("Order {} updated to status {}", orderId, status);
        } catch (Exception e) {
            log.error("Failed to update order: {}", e.getMessage());
            throw e;
        }
    }

    //тупое получение заказов
 /*   public List<Order> getOrders(String uid, String status, boolean isDriver) throws ExecutionException, InterruptedException {
        try {
            List<Order> orders = new ArrayList<>();
            Query query;

            if (isDriver && status != null && List.of("PENDING", "ASSIGNED").contains(status)) {
                //Водитель запрашивает доступные заказы
                query = firestore.collection("orders").whereEqualTo("status", status);
            } else if (isDriver) {
                //Водитель запрашивает свои заказы
                query = firestore.collection("orders").whereEqualTo("driverId", uid);
            } else {
                //Клиент запрашивает свои заказы
                query = firestore.collection("orders").whereEqualTo("clientId", uid);
                if (status != null) {
                    query = query.whereEqualTo("status", status);
                }
            }

            QuerySnapshot querySnapshot = query.get().get();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                orders.add(doc.toObject(Order.class));
            }
            return orders;
        } catch (Exception e) {
            log.error("Failed to get orders: {}", e.getMessage());
            throw e;
        }
    }*/

    //TO-DO:
    //тут какая-то хуйня, надо убрать проверку на uid, потому что манагеры так не смогут брать
    public Order getCurrentOrder(String orderId) throws ExecutionException, InterruptedException {
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


    public List<Order> getOrdersWithStatus(String status) throws ExecutionException, InterruptedException {
        try {
            if (!Arrays.toString(OrderStatus.values()).contains(status)) {
                log.error("Invalid status {}", status);
                throw new IllegalArgumentException("Invalid status");
            }

            //log.info("Orders {}", orders.toString());
            return orderRepository.findByStatus(OrderStatus.valueOf(status));
        } catch (Exception e) {
            log.error("Failed to get active orders: {}", e.getMessage());
            throw e;
        }
    }

    public void putDriverInOrder(String driverId, String orderId) throws ExecutionException, InterruptedException {
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

            driverRepository.save(driver);
            orderRepository.save(order);

            log.info("Driver {} successfully assigned to order {}", driverId, orderId);
        } catch (Exception e) {
            log.error("Failed to put driver {} in order {} error: {}", driverId, orderId, e.getMessage());
            throw e;
        }
    }

}
