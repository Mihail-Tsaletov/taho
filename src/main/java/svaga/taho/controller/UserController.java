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
import org.springframework.web.bind.annotation.*;
import svaga.taho.model.User;
import svaga.taho.service.UserService;

import java.util.Map;
import java.util.Objects;
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

    @PostMapping("/approveDriver")
    public ResponseEntity<String> approveDriverRole(@RequestHeader("Authorization") String authHeader,
                                                @RequestBody Map<String, String> request) throws Exception{
        String idToken = authHeader.replace("Bearer ", "");
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String uid = decodedToken.getUid();
        String driverUid = request.get("driverUid");

        DocumentSnapshot managerDoc = firestore.collection("managers").document("MG" + uid).get().get();
        if(!managerDoc.exists()) {
            log.error("Only managers can approve drivers, manager with id {} does not exist", uid);
            return ResponseEntity.badRequest().body(null);
        }

        DocumentSnapshot userDoc = firestore.collection("users").document(driverUid).get().get();
        if(!userDoc.exists()) {
            log.error("User with uid: {} does not exist", driverUid);
            return ResponseEntity.badRequest().body(null);
        }

        DocumentReference driverRef = firestore.collection("drivers").document(driverUid);
        if(!driverRef.get().get().exists()){
            log.error("Driver with uid: {} does not exist", uid);
            return ResponseEntity.badRequest().body(null);
        }else if(Objects.requireNonNull(driverRef.get().get().get("status")).toString().equals("Approved")) {
            log.error("Driver with uid: {} already approved", uid);
            return ResponseEntity.badRequest().body(null);
        }

        driverRef.set(Map.of(
                "driverId", driverUid,
                "name", userDoc.getString("name"),
                "email", userDoc.getString("email"),
                "phoneNumber", userDoc.getString("phoneNumber"),
                "status", "OFFLINE"
        )).get();
        log.info("Driver {} approved", uid);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approveManager")
    public ResponseEntity<String> approveManager(@RequestHeader("Authorization") String authHeader,
                                                 @RequestBody Map<String, String> request) throws Exception {
        String idToken = authHeader.replace("Bearer ", "");
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        String approvedManagerUid ="MG" + decodedToken.getUid();
        String userUid = request.get("userUid");

        DocumentSnapshot managerDoc = firestore.collection("managers").document(approvedManagerUid).get().get();
        if(!managerDoc.exists()) {
            log.error("Manager with uid: {} does not exist", approvedManagerUid);
            return ResponseEntity.badRequest().body(null);
        }

        DocumentSnapshot userDoc = firestore.collection("users").document(userUid).get().get();
        if(!userDoc.exists()) {
            log.error("User with uid: {} does not exist", userUid);
            return ResponseEntity.badRequest().body(null);
        }

        String resId = userService.createManager(userUid);
        log.info("Manager {} approved", resId);
        return ResponseEntity.ok().build();
    }


}
