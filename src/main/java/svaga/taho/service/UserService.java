package svaga.taho.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import svaga.taho.DTO.RegisterRequest;
import svaga.taho.model.Driver;
import svaga.taho.model.DriverStatus;
import svaga.taho.model.Manager;
import svaga.taho.model.User;
import svaga.taho.repository.IDriverRepository;
import svaga.taho.repository.IManagerRepository;
import svaga.taho.repository.IOrderRepository;
import svaga.taho.repository.IUserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private IOrderRepository orderRepository;
    @Autowired
    private IManagerRepository managerRepository;
    @Autowired
    private IDriverRepository driverRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Пользователь с таким номером уже существует");  // TO:DO сделать проверку на роль водителя,
            // чтоб если уже есть юзер, то можно было
            //зарегать к этой же учетке водилу
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());

        return userRepository.save(user);
    }

    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() ->{
                    log.error("User with phone number {} dont find", phone);
                    return new RuntimeException();
                });
    }

    public String createManager(String uid){
        try {
            String approvedManagerUid = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findById(uid).orElseThrow(() -> {
                log.error("User {} does not exist", uid);
                return new IllegalStateException();
            });


            Manager manager = new Manager();
            manager.setManagerId("MG" + uid);
            manager.setUserId(uid);
            manager.setApprovedManagerUid(approvedManagerUid);
            managerRepository.save(manager);
            log.info("Manager created with id {}, id of creator {}", uid, manager.getApprovedManagerUid());
            return manager.getManagerId();
        } catch (Exception e) {
            log.error("Failed to create manager {}", e.getMessage());
            throw e;
        }
    }

    public List<Driver> getFirstFiveDrivers(){
        try {
            return driverRepository.findTop5ByStatus(DriverStatus.AVAILABLE);
        } catch (Exception e) {
            log.error("Failed to get first five drivers {}", e.getMessage());
            throw e;
        }
    }


}
