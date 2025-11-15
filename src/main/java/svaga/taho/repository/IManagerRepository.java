package svaga.taho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import svaga.taho.model.Manager;

@Repository
public interface IManagerRepository extends JpaRepository<Manager, String> {
    boolean existsByUserId(String currentUserUid);
}
