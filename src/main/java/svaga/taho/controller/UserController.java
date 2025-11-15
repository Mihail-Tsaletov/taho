package svaga.taho.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import svaga.taho.model.Driver;
import svaga.taho.model.DriverStatus;
import svaga.taho.model.User;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IManagerRepository;
import svaga.taho.repository.IUserRepository;
import svaga.taho.service.JwtService;
import svaga.taho.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final static Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final IUserRepository userRepository;
    private final IDriverRepository driverRepository;
    private final IManagerRepository managerRepository;
    private final JwtService jwtService;

    public UserController(
            UserService userService,
            IUserRepository userRepository,
            IDriverRepository driverRepository,
            IManagerRepository managerRepository,
            JwtService jwtService
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.managerRepository = managerRepository;
        this.jwtService = jwtService;
    }

    //@PreAuthorize("hasRole('MANAGER')") //TO:DO потом раскоментить
    @PostMapping("/approveDriver")
    public ResponseEntity<String> approveDriverRole(@RequestBody Map<String, String> request) {
        String currentUserUid = getCurrentUserUid();
        String driverId = request.get("driverId");
        try {
            // Проверка: текущий пользователь — менеджер?
            if (!managerRepository.existsByUserId(currentUserUid)) {
                log.error("Only managers can approve drivers. User {} is not a manager.", currentUserUid);
                return ResponseEntity.badRequest().body("Only managers can approve drivers");
            }

            userService.approveDriver(driverId);

            log.info("Driver {} approved by manager {}", driverId, currentUserUid);
            return ResponseEntity.ok("Driver approved");
        } catch (Exception e) {
            log.error("Error approving driver {}", driverId, e);
            return ResponseEntity.badRequest().body("Error approving driver " + driverId);
        }
    }

    //@PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/approveManager")
    public ResponseEntity<String> approveManager(@RequestBody Map<String, String> request) {
        String currentManagerUid = getCurrentUserUid();
        String userUid = request.get("userUid");

        // Проверка: текущий — менеджер?
        if (!managerRepository.existsByUserId(currentManagerUid)) {
            log.error("Only managers can approve");
            return ResponseEntity.badRequest().body("Only managers can approve");
        }

        // Проверка: пользователь существует?
        if (!userRepository.existsById(userUid)) {
            log.error("User not found");
            return ResponseEntity.badRequest().body("User not found");
        }

        String managerId = userService.createManager(userUid);
        log.info("Manager {} approved by {}", managerId, currentManagerUid);
        return ResponseEntity.ok(managerId);
    }

    //Возвращает максимум 5 свободных водителей
    @GetMapping("/getAvailableDrivers")
    @ResponseBody
    public List<Driver> getAvailableDrivers() throws ExecutionException, InterruptedException {
        try {
            return userService.getFirstFiveDrivers();
        } catch (Exception e) {
            log.error("Can't get available drivers, exception: {}", e.getMessage());
            throw e;
        }

    }

    private String getCurrentUserUid() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();
    }
}
