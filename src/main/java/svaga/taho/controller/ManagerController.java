package svaga.taho.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import svaga.taho.service.OrderService;

@Controller/*("/manager")*/
@RequestMapping("/manager")
public class ManagerController {
    
    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String Orders(Model model) {
        return "assign_orders";
    }
}
