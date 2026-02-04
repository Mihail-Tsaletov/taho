package svaga.taho.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

import java.math.BigDecimal;
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

    //Возвращает всех водителей
    @GetMapping("/getAllDrivers")
    @ResponseBody
    public List<Driver> getAllDrivers() throws ExecutionException, InterruptedException {
        try {
            return userService.getAllDrivers();
        } catch (Exception e) {
            log.error("Can't get all drivers, exception: {}", e.getMessage());
            throw e;
        }

    }

    @GetMapping("/getUser")
    public ResponseEntity<User> getUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String uid = getCurrentUserUid();
            User user = userService.getUserById(uid);
            log.info("Get user with Id: {}", uid);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error while getting user {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/getDriver")
    public ResponseEntity<Driver> getDriver(@RequestHeader("Authorization") String authHeader) {
        try {
            String uid = getCurrentUserUid();
            Driver driver = userService.getDriverByUId(uid);
            log.info("Get user with Id: {}", uid);
            return ResponseEntity.ok(driver);
        } catch (Exception e) {
            log.error("Error while getting user {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/getOnLine") //В зависимости от аргумента либо выход на линию, либо с неё
    public ResponseEntity<String> getOnLine(@RequestHeader("Authorization") String authHeader) {
        try {
            String uid = getCurrentUserUid();
            String driverStatus = userService.getDriverOnLine(uid);
            log.info("Get driver on line/off line with UId: {}, status {}", uid, driverStatus);
            return ResponseEntity.ok(driverStatus);
        } catch (Exception e) {
            log.error("Error while getting user {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Transactional
    protected String getCurrentUserUid() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();
    }
}
