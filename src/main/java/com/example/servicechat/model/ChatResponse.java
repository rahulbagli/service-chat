package com.example.servicechat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private int responseCode;
    private String responseText;
    private String queryIntent;
    private String fileName;
}

