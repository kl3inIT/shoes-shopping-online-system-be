package com.sba.ssos.controller;

import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class Controller {

    private final MessageSource messageSource;

    public Controller(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @GetMapping("/free")
    public Map<String, String> free() {
        return Map.of(
                "message",
                messageSource.getMessage(
                        "test.free",
                        null,
                        LocaleContextHolder.getLocale()
                )
        );
    }

    @GetMapping
    public Map<String, String> hello() {
        return Map.of(
                "message",
                messageSource.getMessage(
                        "test.hello",
                        null,
                        LocaleContextHolder.getLocale()
                )
        );
    }

    @GetMapping("/admin")
    public Map<String, String> adminAccess() {
        return Map.of(
                "message",
                messageSource.getMessage(
                        "test.admin",
                        null,
                        LocaleContextHolder.getLocale()
                )
        );
    }

    @GetMapping("/manager")
    public Map<String, String> managerAccess() {
        return Map.of(
                "message",
                messageSource.getMessage(
                        "test.manager",
                        null,
                        LocaleContextHolder.getLocale()
                )
        );
    }
}
