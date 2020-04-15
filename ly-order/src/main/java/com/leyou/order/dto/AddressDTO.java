package com.leyou.order.dto;

import lombok.Data;

import javax.persistence.Table;

@Data
public class AddressDTO {

    private Long userId;
    private String name;
    private String phone;
    private String state; // 省
    private String city; // 城市
    private String district; // 区
    private String address; // 详细地址
    private String zipCode; // 邮编
    private String alias; // 地址别名
    private Boolean isDefault; // 是否为默认地址

}