package com.leyou.seckill.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum OrderStatusEnum {
    NO_PAY (1, "未付款"),
    PAYED (2, "已付款，未发货"),
    DELIVERED (3, "已发货，未确认"),
    CONFIRMED (4, "已确认，未评价"),
    CLOSED (5, "交易失败，订单关闭"),
    ;
    private int status;
    private String desc;

    public int status() {
        return status;
    }
}
