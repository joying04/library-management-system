package com.wxx.library.common.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    DISABLE(0, "禁用"),
    ENABLE(1, "正常");

    private final Integer code;
    private final String desc;

    UserStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserStatusEnum getByCode(Integer code) {
        for (UserStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
