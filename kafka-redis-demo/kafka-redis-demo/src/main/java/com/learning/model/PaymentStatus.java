package com.learning.model;

public enum PaymentStatus {
    PAID,
    FAILED;

    public static PaymentStatus from(String value) {
        for (PaymentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return null;
    }
}
