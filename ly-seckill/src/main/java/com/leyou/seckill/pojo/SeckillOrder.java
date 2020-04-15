package com.leyou.seckill.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Data
@Table(name = "tb_seckill_order")
public class SeckillOrder implements Serializable {

    @Id
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id; // 订单id
    private Long skuId; // 商品id
    private Double price;
    private Long userId; // 用户id
    private Date createTime; // 订单创建时间
    private Date payTime; // 订单支付时间
    private Integer status; // 订单状态：1、未付款 2、已付款,未发货 3、已发货,未确认 4、交易成功 5、交易关闭
    private String receiver; // 收货人
    private String address; // 地址
    private String phone; // 电话
}
