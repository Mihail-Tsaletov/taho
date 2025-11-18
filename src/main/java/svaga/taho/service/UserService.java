package svaga.taho.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import svaga.taho.DTO.RegisterRequest;
import svaga.taho.model.*;
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

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Пользователь с таким номером уже существует");  // TO:DO сделать проверку на роль водителя,
            // чтоб если уже есть юзер, то можно было
            //зарегать к этой же учетке водилу
        }

        //Если пользователь регается как водитель уже после создания основной учетки
        if(request.getRole().equals(UserRole.DRIVER)) {
            User user = userRepository.findByPhone(request.getPhone()).get(); //Уже существующая запись пользователя
            Driver driver = new Driver();
            driver.setStatus(DriverStatus.PENDING);
            driver.setUserId(user.getId());
            driver.setName(request.getName());
            driver.setPhoneNumber(request.getPhone());
            user.setRole(UserRole.DRIVER);
            userRepository.save(user);
            driverRepository.save(driver);
            return user; //возвращается учетка юзерская
        }

        //Если пользователь регается первый раз
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());
        userRepository.save(user);

        return user;
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


    public void approveDriver(String driverId) {
        // Проверка: запись водителя существует?
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) {
            log.error("Driver with uid: {} does not exist", driverId);
            return;
        }

        if ("OFFLINE".equals(driver.getStatus().toString()) || "AVAILABLE".equals(driver.getStatus().toString())) {
            log.error("Driver with uid: {} already approved", driverId);
            return;
        }

        // Обновляем статус
        driver.setStatus(DriverStatus.OFFLINE);
        driverRepository.save(driver);
    }
}
