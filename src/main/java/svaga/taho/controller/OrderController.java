package svaga.taho.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import svaga.taho.DTO.OrderResponse;
import svaga.taho.model.Order;
import svaga.taho.model.OrderStatus;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IUserRepository;
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
    private final IUserRepository userRepository;
    private final IDriverRepository driverRepository;

    public OrderController(OrderService orderService, IUserRepository userRepository, IDriverRepository driverRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestHeader("Authorization") String authHeader,
                                              @RequestBody Order order) {
        try {
            String uid = getCurrentUserUid();
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
            String uid = getCurrentUserUid();
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
            String uid = getCurrentUserUid();
            String status = request.get("status");

            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            orderService.updateOrderStatus(orderId, OrderStatus.valueOf(status));
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
            String uid = getCurrentUserUid();
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

            orderService.putDriverInOrder(driverId, orderId);
            log.info("Put driver {} in order with Id: {}", driverId, orderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Can't put driver {} in order {} , exception: {}", request.get("driverId"), request.get("orderId"), e.getMessage());
            throw e;
        }
    }

    @GetMapping("/getOrdersByUserIdWithStatuses")
    @ResponseBody
    public ResponseEntity<List<OrderResponse>> getOrdersByUserIdWithStatuses(@RequestParam("statuses") List<OrderStatus> statuses) {
        try {
            String clientId = getCurrentUserUid();
            log.info("Fetching orders for user {} with statuses: {}", clientId, statuses);

            // Если статусы не переданы — возвращаем все
            if (statuses == null || statuses.isEmpty()) {
                statuses = List.of(OrderStatus.values());
            }

            List<Order> orders = orderService.getOrdersByUIDAndStatuses(clientId, statuses);

            // Преобразуем Order + Driver → OrderResponse
            List<OrderResponse> response = orders.stream()
                    .map(order -> {
                        OrderResponse dto = new OrderResponse();
                        dto.setId(order.getOrderId());
                        dto.setStartAddress(order.getStartAddress());
                        dto.setEndAddress(order.getEndAddress());
                        dto.setStatus(order.getStatus());

                        // Достаём водителя, если он назначен
                        if (order.getDriverId() != null) {
                            driverRepository.findByDriverId(order.getDriverId())
                                    .ifPresent(driver -> {
                                        dto.setDriverName(driver.getName());
                                        dto.setDriverPhone(driver.getPhoneNumber());
                                    });
                        }

                        return dto;
                    })
                    .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getCurrentUserUid() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();
    }
}
