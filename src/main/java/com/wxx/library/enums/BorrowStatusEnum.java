package com.wxx.library.enums;


import lombok.Getter;

@Getter
public enum BorrowStatusEnum {
    //枚举常量(状态编码+状态描述)
    BORROWED(1, "借阅中"),
    RETURNED(2, "已归还"),
    OVERDUE(3, "逾期");

    private final Integer code;
    private final String desc;

    BorrowStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    // 根据code获取对应的枚举实例
    public static BorrowStatusEnum getByCode(Integer code) {
        for (BorrowStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null; // 或抛出异常，处理无效code
    }
}
