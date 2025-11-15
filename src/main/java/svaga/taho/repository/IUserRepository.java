package svaga.taho.repository;

import org.springframework.stereotype.Repository;
import svaga.taho.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsById(String id);
    Optional<User> findById(String id);
}