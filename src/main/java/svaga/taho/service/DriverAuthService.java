package svaga.taho.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IUserRepository;

@Service
@RequiredArgsConstructor
public class DriverAuthService {

    private final IUserRepository userRepository;
    private final IDriverRepository driverRepository;

    @Transactional
    public String getDriverIdByCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();

        String uid = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getId();

        return driverRepository.findByUserId(uid)
                .orElseThrow(() -> new IllegalStateException("Driver not found by uid: " + uid))
                .getDriverId();
    }
}
