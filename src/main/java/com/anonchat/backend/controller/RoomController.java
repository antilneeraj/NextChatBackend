package com.anonchat.backend.controller;

import com.anonchat.backend.model.ChatMessage;
import com.anonchat.backend.service.ChatService;
import com.anonchat.backend.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final ChatService chatService;
    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private RoomService roomService;

    // DTO class for the request
    public static class CreateRoomRequest {
        public String roomName;
        public String username;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createRoom(@RequestBody CreateRoomRequest request) {
        // Generate ID and Save Name
        String roomId = roomService.createRoom(request.roomName);

        // Return the details needed for Frontend
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "roomName", request.roomName,
                "inviteLink", "http://localhost:3000/?room=" + roomId
        ));
    }

    // Get Room Info (For friends clicking the link)
    @GetMapping("/{roomId}/info")
    public ResponseEntity<Map<String, String>> getRoomInfo(@PathVariable String roomId) {
        String name = roomService.getRoomName(roomId);
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "roomName", name
        ));
    }

    // Claim Ownership (Called automatically when joining)
    @PostMapping("/{roomId}/claim")
    public ResponseEntity<Map<String, String>> claimRoom(@PathVariable String roomId) {
        String token = chatService.attemptToClaimRoom(roomId);

        if (token != null) {
            return ResponseEntity.ok(Map.of("role", "OWNER", "token", token));
        } else {
            return ResponseEntity.ok(Map.of("role", "GUEST"));
        }
    }

    // Delete Room (Requires Token)
    @DeleteMapping("/{roomId}")
    public ResponseEntity<String> deleteRoom(@PathVariable String roomId,
                                             @RequestHeader("X-Owner-Token") String token) {
        // Security Check
        if (!chatService.verifyOwner(roomId, token))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("🚫 Access Denied: You are not the owner.");

        chatService.deleteRoom(roomId);

        ChatMessage systemMsg = ChatMessage.builder()
                .type(ChatMessage.MessageType.LEAVE)
                .sender("System")
                .content("🚫 Room has been deleted by the owner.")
                .build();

        messagingTemplate.convertAndSend("/topic/" + roomId, systemMsg);


        chatService.deleteRoom(roomId);
        return ResponseEntity.ok("✅ Room deleted successfully.");
    }
}