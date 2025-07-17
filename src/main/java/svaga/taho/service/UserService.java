package svaga.taho.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import svaga.taho.model.User;

import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    public String createUser(User user) throws ExecutionException, InterruptedException {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            if(uid == null) {
                log.error("No authentication user found in context");
                throw new IllegalStateException("User must be authenticated");
            }
            DocumentReference docRef = firestore.collection("users").document(uid);
            user.setUserId(uid);
            docRef.set(user).get();
            log.info("User created with id {}", uid);
            return user.getUserId();
        } catch (Exception e) {
            log.error("Failed to create user {}", e.getMessage());
            throw e;
        }
    }


}
