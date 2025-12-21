package com.anonchat.backend.controller;

import com.anonchat.backend.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    // Handling Chat Messages
    @MessageMapping("/chat/{roomId}/sendMessage")
    @SendTo("/topic/{roomId}")
    public ChatMessage sendMessage(@DestinationVariable String roomId,
                                   @Payload ChatMessage chatMessage) {
        return chatMessage;
    }

    // Handling Join Events
    @MessageMapping("/chat/{roomId}/addUser")
    @SendTo("/topic/{roomId}")
    public ChatMessage addUser(@DestinationVariable String roomId,
                               @Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {

        // Adding username AND roomId in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("roomId", roomId);

        return chatMessage;
    }
}