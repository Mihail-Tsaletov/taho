package svaga.taho.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import svaga.taho.model.Driver;
import svaga.taho.model.DriverStatus;
import svaga.taho.model.Order;
import svaga.taho.model.OrderStatus;
import com.google.cloud.Timestamp;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final Firestore firestore = FirestoreClient.getFirestore();

    public String createOrder(Order order) throws ExecutionException, InterruptedException {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            if (uid == null) {
                log.error("User authentication not found");
                throw new IllegalStateException("User must be authenticated");
            }

            //Проверка роли
            DocumentReference userRef = firestore.collection("users").document(uid);
            DocumentSnapshot userDoc = userRef.get().get();
            if (!userDoc.exists()) {
                log.error("User {} does not exist", uid);
                throw new IllegalStateException("User not found");
            }
            String role = userDoc.getString("role");
            if (!"client".equals(role)) {
                log.error("User {} is not a client", uid);
                throw new IllegalStateException("User is not a client");
            }

            //Валидация заказа
            if (order.getStartPoint() == null || order.getStartPoint().isEmpty() || order.getEndPoint() == null || order.getEndPoint().isEmpty()) {
                log.error("Dont have a start point or end point");
                throw new IllegalArgumentException("Dont have a start point or end point");
            }

            //Создание заказа
            order.setClientId(uid);
            order.setStatus(OrderStatus.PENDING.toString());
            order.setOrderTime(Timestamp.now());
            order.setDriverId(null);
            order.setAcceptanceTime(null);
            order.setAssignedTime(null);
            order.setPickupTime(null);
            order.setDropOffTime(null);

            DocumentReference orderRef = firestore.collection("orders").document();
            order.setOrderId(orderRef.getId());
            orderRef.set(order).get();
            log.info("Order created with id {}", order.getOrderId());
            return order.getOrderId();
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
            throw e;
        }
    }

    public void acceptOrder(String orderId) throws ExecutionException, InterruptedException {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            if (uid == null) {
                log.error("Driver authentication not found");
                throw new IllegalStateException("Driver must be authenticated");
            }

            //Проверка водителя
            DocumentReference driverRef = firestore.collection("drivers").document(uid);
            DocumentSnapshot driverDoc = driverRef.get().get();
            if (!driverDoc.exists()) {
                log.error("Driver {} not found", uid);
                throw new IllegalStateException("Driver not found");
            }
            Driver driver = driverDoc.toObject(Driver.class);
            if (!"AVAILABLE".equals(driver.getStatus())) {
                log.error("Driver {} is not available, current status: {}", uid, driver.getStatus());
                throw new IllegalStateException("Driver is not available");
            }

            //Проверка заказа
            DocumentReference orderRef = firestore.collection("orders").document(orderId);
            DocumentSnapshot orderDoc = orderRef.get().get();
            if (!orderDoc.exists()) {
                log.error("Order {} does not exist", orderId);
                throw new IllegalArgumentException("Order not found");
            }
            Order order = orderDoc.toObject(Order.class);
            if (!"PENDING".equals(order.getStatus()) && !"ASSIGNED".equals(order.getStatus())) {
                log.error("Order {} is not ASSIGNED or PENDING, current status: {}", orderId, order.getStatus());
                throw new IllegalStateException("Order cannot be accepted");
            }

            //Обновление статуса заказа
            order.setDriverId(uid);
            order.setStatus(OrderStatus.ACCEPTED.toString());
            order.setAcceptanceTime(Timestamp.now());

            //Обновление статуса водителя
            driver.setStatus(DriverStatus.BUSY.toString());
            driverRef.set(driver).get();

            orderRef.set(order).get();
            log.info("Order {} accepted by driver {}", orderId, uid);
        } catch (Exception e) {
            log.error("Failed to accept order: {}", e.getMessage());
            throw e;
        }
    }

    public void updateOrder(String orderId, String status) throws ExecutionException, InterruptedException {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            if (uid == null) {
                log.error("No authenticated user found");
                throw new IllegalStateException("User must be authenticated");
            }

            DocumentReference orderRef = firestore.collection("orders").document(orderId);
            DocumentSnapshot orderDoc = orderRef.get().get();
            if (!orderDoc.exists()) {
                log.error("Order {} does not found", orderId);
                throw new IllegalArgumentException("Order not found");
            }
            Order order = orderDoc.toObject(Order.class);
            if (!Arrays.toString(OrderStatus.values()).contains(order.getStatus())) {
                log.error("Invalid status {}", status);
                throw new IllegalArgumentException("Invalid status");
            }

            //Обновление времени
            if (OrderStatus.ACCEPTED.toString().equals(status)) {
                order.setAcceptanceTime(Timestamp.now());
            } else if (OrderStatus.PICKED_UP.toString().equals(status)) {
                order.setPickupTime(Timestamp.now());
            } else if (OrderStatus.COMPLETED.toString().equals(status)) {
                order.setDropOffTime(Timestamp.now());
            }

            order.setStatus(status);
            orderRef.set(order).get();
            log.info("Order {} updated to status {}", orderId, status);
        } catch (Exception e) {
            log.error("Failed to update order: {}", e.getMessage());
            throw e;
        }
    }

}
