package com.sba.ssos.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
    PENDING("Đang chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Đã từ chối");

    private final String description;
}
