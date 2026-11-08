package org.example.dto;

import lombok.Data;

/**
 * 用户聊天类
 */
@Data
public class ChatRequestDto {
    private String sessionId;
    private String type;
    private String message;
    private String timestamp;
}