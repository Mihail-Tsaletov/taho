package svaga.taho.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import svaga.taho.model.Order;
import svaga.taho.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
/*
POST /api/orders: Создание заказа (только для клиентов).
PUT /api/orders/{id}/accept: Принятие заказа водителем.
PUT /api/orders/{id}/status: Обновление статуса заказа (клиент или водитель).
GET /api/orders: Получение списка заказов (для клиента — свои заказы, для водителя — доступные заказы).
GET /api/orders/{id}: Получение информации о конкретном заказе.
*/

@Controller
@RequestMapping("/api/orders")
public class OrderController {
    private final static Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;
    private final Order order;

    public OrderController(OrderService orderService, Order order) {
        this.orderService = orderService;
        this.order = order;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody Order order) {
        try {
            String uid = decodeToken(authHeader);
            String orderId = orderService.createOrder(order, uid);
            log.info("Created order with Id: {}, by user: {}", orderId, uid);
            return ResponseEntity.ok(orderId);
        } catch (Exception e) {
            log.error("Error while creating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to create order: " + e.getMessage());
        }
    }


    //Принятие заказа ВОДИТЕЛЕМ
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptOrder(@RequestHeader("Authorization") String authHeader,
                                            @PathVariable("id") String orderId) {
        try {
            String uid = decodeToken(authHeader);
            orderService.acceptOrder(orderId, uid);
            log.info("Accepted order with Id: {}, by driver: {}", orderId, uid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error while accept order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    //Обновление статуса заказа
    @PutMapping("/{id}/updateStatus")
    public ResponseEntity<Void> updateOrderStatus(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable("id") String orderId,
                                                  @RequestBody Map<String, String> request) {
        try {
            String uid = decodeToken(authHeader);
            String status = request.get("status");

            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            orderService.updateOrder(orderId, status);
            log.info("Updated order with Id: {} to status {}, by user: {}", orderId, status, uid);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error while updating order status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

/*    //тупое получение заказов
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(@RequestHeader("Authorization") String authHeader,
                                                 @RequestBody Map<String, String> request) {
        try {
            String uid = decodeToken(authHeader);
            String status = request.get("status");
            boolean isDriver = Boolean.parseBoolean(request.get("isDriver"));

            List<Order> orders = orderService.getOrders(uid, status, isDriver);
            log.info("Get orders {}, for user: {}", orders.size(), uid);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Error while getting orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }*/

    //получение конкретного заказа
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@RequestHeader("Authorization") String authHeader,
                                          @PathVariable("id") String orderId) {
        try {
            String uid = decodeToken(authHeader);
            Order order = orderService.getCurrentOrder(orderId);
            log.info("Get order with Id: {}, by user: {}", orderId, uid);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Error while getting order {}: {}", orderId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    //получение заказов с конкретным статусом
    @GetMapping("/getOrdersWithStatus")
    @ResponseBody
    public List<Order> getOrdersWithStatus(@RequestParam String status) throws ExecutionException, InterruptedException {
        return orderService.getOrdersWithStatus(status);
    }

    @PostMapping("/putDriverInOrder")
    public ResponseEntity<Void> putDriverInOrder(@RequestBody Map<String, String> request) throws Exception {
        try {
            String driverId = request.get("driverId");
            String orderId = request.get("orderId");

            orderService.putDriverInOrder(driverId,orderId);
            log.info("Put driver {} in order with Id: {}",driverId, orderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Can't put driver {} in order {} , exception: {}", request.get("driverId"), request.get("orderId"),  e.getMessage());
            throw e;
        }
    }

    private String decodeToken(String authHeader) throws Exception {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return decodedToken.getUid();
        } catch (Exception e) {
            log.error("Can't decode header: {}, exception: {}", authHeader, e.getMessage());
            throw e;
        }
    }
}
