package svaga.taho.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import svaga.taho.model.User;
import svaga.taho.service.UserService;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) throws ExecutionException, InterruptedException {
        String userId = userService.createUser(user);
        return ResponseEntity.ok(userId);
    }
}
