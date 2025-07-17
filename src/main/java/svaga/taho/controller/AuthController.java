package svaga.taho.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import svaga.taho.service.UserService;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final static Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private FirebaseAuth firebaseAuth;
    @Autowired
    private Firestore firestore;

    @Value("${web.api.key}")
    private String WEB_API_KEY;
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String role = request.getOrDefault("role", "client");
        String phone = request.get("phone");
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty() || phone == null || phone.isEmpty()) {
            log.error("Missing required fields: email, password or phone");
            return ResponseEntity.badRequest().body("Email, password and phone are required");
        }

        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setPhoneNumber(phone);
            UserRecord user = firebaseAuth.createUser(createRequest);
            log.info("Created user with UID: {}", user.getUid());

            DocumentReference docRef = firestore.collection("users").document(user.getUid());
            Map<String, Object> userData = Map.of(
                    "id", user.getUid(),
                    "email", email,
                    "name", name,
                    "phoneNumber", phone,
                    "role", role
            );
            docRef.set(userData).get();
            log.info("User data saved to firestore with ID: {}", user.getUid());


            String url = String.format("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s", WEB_API_KEY);
            String payload = String.format("{\"email\": \"%s\", \"password\": \"%s\", \"returnSecureToken\": true}",
                    email, password);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                String idToken = jsonResponse.get("idToken").asText();
                log.info("IDToken generated for user: {}", email);
                return ResponseEntity.ok(idToken);
            } else {
                log.error("Failed to generate IDToken: {}", response.body());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to generate IDToken");
            }

        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            log.error("Missing required fields: email, password");
            return ResponseEntity.badRequest().body("Email, password and phone are required");
        }

        try {
            String url = String.format("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=%s", WEB_API_KEY);
            String payload = String.format("{\"email\": \"%s\", \"password\": \"%s\", \"returnSecureToken\": true}",
                    email, password);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                String idToken = jsonResponse.get("idToken").asText();
                log.info("User logged with email: {}", email);
                return ResponseEntity.ok(idToken);
            } else {
                log.error("Login failed: {}", response.body());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed");
            }
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed" + e.getMessage());
        }
    }
}
