package svaga.taho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import svaga.taho.service.OrderService;

@Controller("/manager")
public class ManagerController {
    
    @Autowired
    private OrderService orderService;
}
