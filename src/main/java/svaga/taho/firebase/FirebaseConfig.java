package svaga.taho.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);
    @Value("${firebase.service-account-file}")
    private Resource serviceAccountFile;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        FirebaseConfig.log.info("Attempting to load Firebase service account file: {}", serviceAccountFile.getFilename());
        if (!serviceAccountFile.exists()) {
            FirebaseConfig.log.error("Service account file does not exist: {}", serviceAccountFile.getFilename());
            throw new IOException("Firebase service account file not found");
        }
        FirebaseConfig.log.info("Service account file path: {}", serviceAccountFile.getURI());
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountFile.getInputStream()))
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(options);
        FirebaseConfig.log.info("Firebase initialized successfully");
        return app;
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        try{
            Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
            log.info("Firestore initialized successfully");
            return firestore;
        }
        catch (Exception e){
            log.error("Firestore initialization failed", e);
            throw new RuntimeException("Firestore initialization failed", e);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
