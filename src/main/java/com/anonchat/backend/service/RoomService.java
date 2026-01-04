package com.anonchat.backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Duration;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LEN = 8;

    public String generateRoomId() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(ID_LEN);
        for (int i = 0; i < ID_LEN; i++)
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));

        return sb.toString();
    }

    // Create room and map ID -> Name
    public String createRoom(String roomName) {
        String uniqueId = generateRoomId();

        // Save the human-readable name in Redis (Expires in 1 hour if idle)
        String key = "room:" + uniqueId + ":name";
        redisTemplate.opsForValue().set(key, roomName, Duration.ofHours(1));

        return uniqueId;
    }

    // NEW: Get Name from ID (for joiners)
    public String getRoomName(String roomId) {
        String name = redisTemplate.opsForValue().get("room:" + roomId + ":name");
        return name != null ? name : "Unknown Room";
    }
}