package com.monika.accounts.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountsController {
    @GetMapping("/sayHello")
    public String hello(){
        return "Hello World";
    }

}
