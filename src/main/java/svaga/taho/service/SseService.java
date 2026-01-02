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
    public void notifyDriverAboutOrder(String driverId, Map<String, Object> data) {
        SseEmitter emitter = driverEmitters.get(driverId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .data(data));

                log.info("Sent new order {} to driver {}", data.get("id"), driverId);

            } catch (Exception e) {
                log.warn("Failed to send to driver {}: {}", driverId, e.getMessage());
                driverEmitters.remove(driverId);
            }
        } else {
            log.info("Driver {} not connected (SSE missed)", driverId);
        }
    }

    // Клиент подписывается на обновления конкретного заказа
    public SseEmitter subscribeToOrder(@PathVariable String orderId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

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
                        .data(data));
                log.info("Событие отправлено для заказа {}", data);
            } catch (IOException e) {
                emitters.remove(orderId);
            }
        }
    }

}
