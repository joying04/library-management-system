package com.wxx.library.common.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    COMMON(0, "普通用户"),
    ADMIN(1, "管理员");

    private final Integer code;
    private final String desc;

    UserRoleEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRoleEnum getByCode(Integer code) {
        for (UserRoleEnum role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }
}
