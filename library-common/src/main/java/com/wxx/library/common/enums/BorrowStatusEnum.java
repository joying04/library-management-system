package com.wxx.library.common.enums;

import lombok.Getter;

@Getter
public enum BorrowStatusEnum {
    BORROWED(1, "借阅中"),
    RETURNED(2, "已归还"),
    OVERDUE(3, "逾期");

    private final Integer code;
    private final String desc;

    BorrowStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BorrowStatusEnum getByCode(Integer code) {
        for (BorrowStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
