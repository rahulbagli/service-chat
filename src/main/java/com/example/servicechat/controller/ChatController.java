package com.example.servicechat.controller;

import com.example.servicechat.model.ChatResponse;
import com.example.servicechat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.invoke.MethodHandles;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
@Controller
public class ChatController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ChatService chatService;

    @RequestMapping(value = "/query",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            consumes = { MediaType.APPLICATION_JSON_VALUE },
            method = RequestMethod.POST)
    public ResponseEntity<ChatResponse> chat(@RequestBody String message) {
        LOGGER.info("Received message: {}", message);
        return ResponseEntity.ok(chatService.handleUserMessage(message.trim()));
    }


}
