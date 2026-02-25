package com.sba.ssos.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    SYSTEM("System"),
    PROMOTION("Promotion"),
    ORDER("Order"),
    PAYMENT("Payment"),
    DELIVERY("Delivery");

    private final String description;
}

