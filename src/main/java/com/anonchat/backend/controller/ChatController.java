package com.anonchat.backend.controller;

import com.anonchat.backend.model.ChatMessage;
import com.anonchat.backend.service.ChatService;
import com.anonchat.backend.service.RateLimitService;
import com.anonchat.backend.service.FilterService;
import com.anonchat.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final RateLimitService rateLimitService;
    private final FilterService filterService;

    // Handling Chat Messages
    @MessageMapping("/chat/{roomId}/sendMessage")
    @SendTo("/topic/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId,
                                   @Payload ChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor) {
        validateRoomId(roomId);

        String ipAddress = (String) headerAccessor.getSessionAttributes().get("IP_ADDRESS");

        if (ipAddress != null && !rateLimitService.allowRequest(ipAddress)) {
            return ChatMessage.builder()
                    .type(ChatMessage.MessageType.CHAT)
                    .sender("System")
                    .content("🚫 Slow down! You are typing too fast.")
                    .build();
        }

        // CONTENT CHECK: Profanity Filtering
        String cleanContent = filterService.sanitize(chatMessage.getContent());
        chatMessage.setContent(cleanContent);

        chatService.saveMessage(roomId, chatMessage);

        return chatMessage;
    }

    // Handling Join Events
    @MessageMapping("/chat/{roomId}/addUser")
    @SendTo("/topic/{roomId}")
    public ChatMessage addUser(@DestinationVariable String roomId,
                               @Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        validateRoomId(roomId);

        String username = chatMessage.getSender();
        if (!filterService.isValidUsername(username)) {
            // Force rename to something harmless
            String safeName = "Guest_" + System.currentTimeMillis() % 10000;
            chatMessage.setSender(safeName);
        } else {
            String cleanSender = filterService.sanitize(username);
            chatMessage.setSender(cleanSender);
        }

        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setContent("joined the room.");

//        String ipAddress = (String) headerAccessor.getSessionAttributes().get("IP_ADDRESS");
//        if (rateLimitService.isBanned(ipAddress)) {
            // ... (Ban logic) ...
//        }

        // Adding username AND roomId in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        // The "Join" event is also the part of history
        chatService.saveMessage(roomId, chatMessage);

        return chatMessage;
    }

    private void validateRoomId(String roomId) {
        // Only allow Alphanumeric, underscores, and hyphens (e.g., "room-1", "MyChat_2")
        ValidationUtils.validateRoomId(roomId);
    }
}