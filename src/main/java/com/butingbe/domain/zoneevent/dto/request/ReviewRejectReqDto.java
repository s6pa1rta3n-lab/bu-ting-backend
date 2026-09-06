package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for rejecting a participation under review. */
public record ReviewRejectReqDto(
    @NotBlank(message = "반려 사유는 필수입니다.") @Size(max = 500) String reason) {}
