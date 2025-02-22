package com.github.sergeynik.horizondashboard.controller.vew;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String getTransactions() {
        return "index";
    }
}
