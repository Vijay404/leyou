package com.leyou.order.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Table(name = "tb_addr")
public class UserAddress {
    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id; // 地址id
    private Long userId; // 用户id
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
