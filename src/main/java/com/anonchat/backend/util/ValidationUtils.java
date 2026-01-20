package com.anonchat.backend.util;

public class ValidationUtils {

    private static final String ROOM_ID_REGEX = "^[a-zA-Z0-9_-]+$";

    public static void validateRoomId(String roomId) {
        if (roomId == null || !roomId.matches(ROOM_ID_REGEX)) {
            throw new IllegalArgumentException("Invalid Room ID. Only letters, numbers, underscores, and dashes allowed.");
        }
    }
}