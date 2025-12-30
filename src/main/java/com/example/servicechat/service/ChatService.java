package com.example.servicechat.service;

import com.example.servicechat.model.ChatResponse;

public interface ChatService {
    ChatResponse handleUserMessage(String message);


}
