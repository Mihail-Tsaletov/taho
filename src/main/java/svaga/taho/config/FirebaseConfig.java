package svaga.taho.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Value("${firebase.service-account-file}")
    private Resource serviceAccountFile;

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        System.out.println("Attempting to load Firebase service account file: " + serviceAccountFile.getFilename());
        if (!serviceAccountFile.exists()) {
            System.err.println("Service account file does not exist: " + serviceAccountFile.getFilename());
            throw new IOException("Firebase service account file not found");
        }
        System.out.println("Service account file path: " + serviceAccountFile.getURI());
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountFile.getInputStream()))
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(options);
        System.out.println("Firebase initialized successfully");
        return app;
    }
}
