package svaga.taho.controller;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import svaga.taho.model.User;
import svaga.taho.service.UserService;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final static Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final Firestore firestore;

    public UserController(UserService userService, Firestore firestore) {
        this.userService = userService;
        this.firestore = firestore;
    }

    public ResponseEntity<String> createUser(@RequestBody User user) throws ExecutionException, InterruptedException {
        String userId = userService.createUser(user);
        return ResponseEntity.ok(userId);
    }

    @PostMapping("/addDriver")
    public ResponseEntity<String> addDriverRole(@RequestHeader("Authorization") String authHeader,
                                                @RequestBody Map<String, String> request) throws Exception{
        String idToken = authHeader.replace("Bearer ", "");
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();

        DocumentSnapshot userDoc = firestore.collection("users").document(uid).get().get();
        if(!userDoc.exists()) {
            log.error("User with uid: {} does not exist", uid);
            return ResponseEntity.badRequest().body(null);
        }

        DocumentReference driverRef = firestore.collection("drivers").document(uid);
        if(driverRef.get().get().exists()) {
            log.error("Driver with uid: {} already exists", uid);
            return ResponseEntity.badRequest().body(null);
        }

        driverRef.set(Map.of(
                "driverId", uid,
                "name", userDoc.getString("name"),
                "email", userDoc.getString("email"),
                "phoneNumber", userDoc.getString("phoneNumber"),
                "status", "OFFLINE"
        )).get();
        return ResponseEntity.ok().build();
    }
}
