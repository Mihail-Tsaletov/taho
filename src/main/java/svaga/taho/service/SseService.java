package svaga.taho.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import svaga.taho.model.Order;
import svaga.taho.model.OrderStatus;
import svaga.taho.model.User;
import svaga.taho.repository.IUserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SseService {
    // Хранилище активных соединений: orderId → SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    // Хранилище активных соединений: uid → SseEmitter
    private final ConcurrentHashMap<String, SseEmitter> driverEmitters = new ConcurrentHashMap<>();

    private final IUserRepository userRepository;

    public SseService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SseEmitter subscribeDriver(String driverId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        driverEmitters.put(driverId, emitter);

        emitter.onCompletion(() -> driverEmitters.remove(driverId));
        emitter.onTimeout(() -> driverEmitters.remove(driverId));
        emitter.onError((ex) -> driverEmitters.remove(driverId));

        log.info("Driver {} connected to SSE", driverId);
        return emitter;
    }

    // Вызывается из OrderService, когда менеджер назначает заказ водителю
    public void notifyDriverAboutOrder(String driverId, Order order) {
        SseEmitter emitter = driverEmitters.get(driverId);
        if (emitter != null) {
            try {
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

                emitter.send(SseEmitter.event()
                        .name("assigned-order")
                        .data(data));

                log.info("Sent new order {} to driver {}", order.getOrderId(), driverId);

            } catch (Exception e) {
                log.warn("Failed to send to driver {}: {}", driverId, e.getMessage());
                driverEmitters.remove(driverId);
            }
        } else {
            log.info("Driver {} not connected (SSE missed)", driverId);
        }
    }

    private String getClientPhone(String clientId) {
        return userRepository.findById(clientId)
                .map(User::getPhone)
                .orElse("Неизвестно");
    }
    private String getClientName(String clientId) {
        return userRepository.findById(clientId)
                .map(User::getName)
                .orElse("Неизвестно");
    }



    // Клиент подписывается на обновления конкретного заказа
    public SseEmitter subscribeToOrder(@PathVariable String orderId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(orderId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE отключён для заказа {}", orderId);
            emitters.remove(orderId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE таймаут для заказа {}", orderId);
            emitters.remove(orderId);
        });
        emitter.onError((ex) -> emitters.remove(orderId));

        // Отправляем сразу текущий статус
        log.info("Order {} subscribed", orderId);
        return emitter;
    }

    // Метод для отправки обновлений (вызывается из OrderService)
    public void sendOrderUpdate(String orderId, Map<String, Object> data) {
        SseEmitter emitter = emitters.get(orderId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("order-update")
                        .data(data));
                log.info("Событие отправлено для заказа {}", data);
            } catch (IOException e) {
                emitters.remove(orderId);
            }
        }
    }
}
