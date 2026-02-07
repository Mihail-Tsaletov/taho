package svaga.taho.controller;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import svaga.taho.repository.IUserRepository;
import svaga.taho.service.OrderService;
import svaga.taho.service.UserService;

import java.math.BigDecimal;

@Controller/*("/manager")*/
@RequestMapping("/manager")
public class ManagerController {
    private final static Logger log = LoggerFactory.getLogger(ManagerController.class);
    private final UserService userService;
    private final IUserRepository userRepository;

    public ManagerController(OrderService orderService, UserService userService, IUserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }


    @GetMapping("/orders")
    public String Orders(Authentication authentication, Model model) {
        return "assign_orders";
    }

    @GetMapping("/stat")
    public String Stats(Model model) {
        model.addAttribute("currentUserName", getCurrentUserName());
        return "manager_stats";
    }

    @PostMapping("/addBalance")
    public ResponseEntity<String> addBalance(@RequestParam("driverPhoneNumber") String driverPhoneNumber,
                                             @RequestParam("value") String value) {
        try {
            double balance = userService.monetaNaHodNogi(driverPhoneNumber, BigDecimal.valueOf(Double.parseDouble(value)));
            log.info("Add money on balance to driver: {}, balance {}", driverPhoneNumber, balance);
            return ResponseEntity.ok(String.valueOf(balance));
        } catch (Exception e) {
            log.error("Error while add balance to driver {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @Transactional
    protected String getCurrentUserName() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalStateException("User not found by phone: " + phone))
                .getName();
    }
}
