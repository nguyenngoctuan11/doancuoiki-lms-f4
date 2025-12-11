package com.example.back_end.model.enums;

public enum SupportSenderType {
    STUDENT,
    MANAGER,
    SYSTEM,
    BOT;

    public static SupportSenderType from(String value) {
        if (value == null) return null;
        try {
            return SupportSenderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
