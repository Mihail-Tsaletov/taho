package svaga.taho.model;

import jakarta.persistence.*;
import lombok.Data;
import svaga.taho.model.UserRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @Column(name = "id", length = 50, nullable = false, insertable = true, updatable = false)
    private String id;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean enabled = true;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
    }
}