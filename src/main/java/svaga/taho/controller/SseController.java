package svaga.taho.controller;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IUserRepository;
import svaga.taho.service.SseService;

@RestController
@RequestMapping("/api/sse")
public class SseController {

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
        return sseService.subscribeToOrder(orderId);
    }

    @GetMapping(value = "/subscribe/driver", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter driverStream(Authentication auth) {
        String driverId = getDriverId();
        return sseService.subscribeDriver(driverId);
    }

    private String getCurrentUserUid() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();
    }
    private String getDriverId() {
        String uid = getCurrentUserUid();
        return driverRepository.findByUserId(uid)
                .orElseThrow(() -> new IllegalStateException("Driver not found by uid: " + uid))
                .getDriverId();
    }
}