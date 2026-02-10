package com.sba.ssos.enums;

import jakarta.annotation.Nullable;

public enum PaymentMethod {

    ONLINE("PENDING"),
    COD("COD");


    private final String id;

    PaymentMethod(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static PaymentMethod fromId(String id) {
        if (id == null) {
            return null;
        }
        for (PaymentMethod status : PaymentMethod.values()) {
            if (status.getId().equalsIgnoreCase(id)) {
                return status;
            }
        }
        return null;
    }
}
