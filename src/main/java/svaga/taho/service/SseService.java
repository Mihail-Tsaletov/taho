package svaga.taho.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    // Клиент подписывается на обновления конкретного заказа
    public SseEmitter subscribeToOrder(@PathVariable String orderId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 = без таймаута TO:DO исправить эту задержку

        emitters.put(orderId, emitter);

        emitter.onCompletion(() -> emitters.remove(orderId));
        emitter.onTimeout(() -> emitters.remove(orderId));
        emitter.onError((ex) -> emitters.remove(orderId));

        // Отправляем сразу текущий статус
        sendOrderUpdate(orderId, Map.of("message", "Привет от сервера! Подключение работает!", "time", LocalDateTime.now().toString()));
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
