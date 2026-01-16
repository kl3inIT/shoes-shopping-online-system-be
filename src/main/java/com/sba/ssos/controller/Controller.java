package com.sba.ssos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class Controller {

    @GetMapping("/free")
    public String free() {
        return "Hello";
    }

    @GetMapping
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/admin")
    public String adminAccess() {
        return "Hello admin";
    }
}
