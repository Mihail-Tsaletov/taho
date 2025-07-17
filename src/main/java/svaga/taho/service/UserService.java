package svaga.taho.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        DocumentReference docRef = firestore.collection("users").document();
        user.setUserId(docRef.getId());
        docRef.set(user).get();
        log.info("User created with id {}", user.getUserId());
        return user.getUserId();
    }


}
