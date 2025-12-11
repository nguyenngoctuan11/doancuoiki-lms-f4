package com.example.back_end.model.enums;

public enum SupportThreadStatus {
    NEW,
    IN_PROGRESS,
    WAITING_STUDENT,
    CLOSED;

    public static SupportThreadStatus from(String value) {
        if (value == null) return null;
        try {
            return SupportThreadStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
