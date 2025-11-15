package svaga.taho.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.stereotype.Component;

@Entity
@Table(name = "managers")
@Data
public class Manager {
    @Id
    @Column(name = "manager_id", length = 50)
    private String managerId;

    @Column(name = "user_id", length = 50, unique = true)
    private String userId;

    private String approvedManagerUid;
}
