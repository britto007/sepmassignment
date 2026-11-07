package com.kuet.ums.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(int status, String error, String message, Map<String, String> fields) {

    public ApiError(int status, String error, String message) {
        this(status, error, message, Map.of());
    }
}
