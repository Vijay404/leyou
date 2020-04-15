package com.leyou.order.enums;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public enum OrderStatusEnum {
    NO_PAY (1, "未付款"),
    PAYED (2, "已付款，未发货"),
    DELIVERED (3, "已发货，未确认"),
    CONFIRMED (4, "已确认，未评价"),
    CLOSED (5, "交易失败，订单关闭"),
    RATED (6, "已评价"),
    REFUND (7, "退款"),

    ;
    private int status;
    private String desc;

    public int status() {
        return status;
    }
}
