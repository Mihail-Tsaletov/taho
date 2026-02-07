package svaga.taho.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebAuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";   // templates/login.html
    }
}