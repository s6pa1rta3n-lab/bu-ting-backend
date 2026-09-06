package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for reporting an inappropriate event participation. */
public record ReportCreateReqDto(
    @NotBlank(message = "신고 사유 코드는 필수입니다.") @Size(max = 50) String reasonCode,
    @Size(max = 500) String reasonDetail) {}
