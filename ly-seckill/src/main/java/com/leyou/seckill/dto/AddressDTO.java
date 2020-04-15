package com.leyou.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {

    @NotNull
    private Long skuId; // 商品id
    private String receiver; // 收货人
    private String address; // 地址
    private String phone; // 电话
}
