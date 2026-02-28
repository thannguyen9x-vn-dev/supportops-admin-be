package com.supportops.api.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDetail(String code, String message, List<String> details, String traceId) {}
