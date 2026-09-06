package com.butingbe.domain.zoneevent.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for creating a comment on an event participation. */
public record CommentCreateReqDto(
    @NotBlank(message = "댓글 내용은 필수입니다.") @Size(max = 500, message = "댓글은 최대 500자까지 작성할 수 있습니다.")
        String content) {}
