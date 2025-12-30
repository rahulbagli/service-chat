package com.example.servicechat.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class ServiceMatch {
    private String serviceId;
    private float score;
    private float percentage;
    private boolean exactMatch;
}
