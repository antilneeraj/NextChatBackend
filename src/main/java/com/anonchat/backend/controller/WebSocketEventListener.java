package com.anonchat.backend.controller;

import com.anonchat.backend.model.ChatMessage;
import com.anonchat.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("Disconnection event received, but session attributes are missing.");
            return;
        }
        String username = (String) sessionAttributes.get("username");
        String roomId = (String) sessionAttributes.get("roomId");

        if (chatService.isRoomBeingDeleted(roomId))
            return; // Exit silently. No "Ghost History"

        if (username != null && roomId != null) {
            log.info("User Disconnected: {} from Room: {}", username, roomId);

            var chatMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.LEAVE)
                    .sender(username)
                    .content("left the room.")
                    .build();

            // Saves Leave event to history
            chatService.saveMessage(roomId, chatMessage);

            // Update user count
            chatService.userLeft(roomId);

            messagingTemplate.convertAndSend("/topic/" + roomId, chatMessage);
        }
    }
}