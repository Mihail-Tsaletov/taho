package svaga.taho.controller;

import jakarta.transaction.Transactional;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IUserRepository;
import svaga.taho.service.SseService;

@RestController
@RequestMapping("/api/sse")
public class SseController {
    private final static Logger log = LoggerFactory.getLogger(SseController.class);

    @Autowired
    private SseService sseService;
    private final IUserRepository userRepository;
    private final IDriverRepository driverRepository;

    public SseController(IUserRepository userRepository, IDriverRepository driverRepository) {
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    @GetMapping(value = "/subscribe/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String orderId) {
        log.info("Subscribing to order {}", orderId);
        return sseService.subscribeToOrder(orderId);
    }

    @GetMapping(value = "/subscribe/driver", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter driverStream(@RequestHeader("Authorization") String authHeader) {
        try {

        String driverId = getDriverId();
        log.info("Subscribing to drivers sse by driver {}", driverId);
        return sseService.subscribeDriver(driverId);}
        catch (Exception e) {
            log.error("Error while subscribing driver: {}", e.getMessage());
            return sseService.subscribeDriver(getDriverId());
        }
    }

    @Transactional
    protected String getCurrentUserUid() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();
    }

    @Transactional
    protected String getDriverId() {
        String uid = getCurrentUserUid();
        return driverRepository.findByUserId(uid)
                .orElseThrow(() -> new IllegalStateException("Driver not found by uid: " + uid))
                .getDriverId();
    }
}