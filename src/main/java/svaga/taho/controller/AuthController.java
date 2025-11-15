// src/main/java/svaga/taho/controller/AuthController.java
package svaga.taho.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svaga.taho.DTO.LoginRequest;
import svaga.taho.DTO.RegisterRequest;
import svaga.taho.model.User;
import svaga.taho.model.UserRole;
import svaga.taho.service.JwtService;
import svaga.taho.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final static Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // По умолчанию — CLIENT
            if (request.getRole() == null) {
                request.setRole(UserRole.CLIENT);
            }

            User user = userService.register(request);
            String token = jwtService.generateToken(user.getPhone(), user.getRole().toString());

            log.info("User registered successful: " + user.getPhone());
            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful!",
                    "token", token,
                    "role", user.getRole()
            ));
        } catch (Exception e) {
            log.error("Can't register user {}", request.getPhone());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.findByPhone(request.getPhone());
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Incorrect password!"));
            }

            String token = jwtService.generateToken(user.getPhone(), user.getRole().toString());

            log.info("User login successful: {} token: {}",user.getPhone(), token);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        } catch (Exception e) {
            log.error("Can't login user {}", request.getPhone());
            return ResponseEntity.status(401).body(Map.of("error", "Пользователь не найден"));
        }
    }
}