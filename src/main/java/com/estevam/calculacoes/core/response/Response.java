package com.estevam.calculacoes.core.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Response<T> {
    private Boolean success;
    private int status;
    private String type;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public Response(Boolean success, int status, String type, String message, T data) {
        this.type = type;
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.success = success;
        this.data = data;
    }
}


