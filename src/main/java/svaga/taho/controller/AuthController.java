// src/main/java/svaga/taho/controller/AuthController.java
package svaga.taho.controller;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

            return ResponseEntity.ok(Map.of(
                    "message", "Регистрация успешна",
                    "token", token,
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.findByPhone(request.getPhone());
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Неверный пароль"));
            }

            String token = jwtService.generateToken(user.getPhone(), user.getRole().toString());

            log.info("User login with phone: {}", request.getPhone());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole(),
                    "name", user.getName()
            ));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Пользователь не найден"));
        }
    }
}