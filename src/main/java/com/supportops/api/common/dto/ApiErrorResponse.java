package com.supportops.api.common.dto;

public record ApiErrorResponse(ApiErrorDetail error) {

    public static ApiErrorResponse of(ApiErrorDetail error) {
        return new ApiErrorResponse(error);
    }
}
