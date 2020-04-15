package com.leyou.seckill.pojo;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Table(name = "tb_sku_seckill")
public class SeckillGoods implements Serializable {

    /**
     * 秒杀商品的id
     */
    @Id
    private Long skuId;

    /**
     * 秒杀开始时间
     */
    private Date startTime;
    /**
     * 秒杀结束时间
     */
    private Date endTime;
    /**
     * 秒杀价格
     */
    private Double seckillPrice;

    // 商品原价
    private Long lastPrice;
    /**
     * 商品标题
     */
    private String title;
 
    /**
     * 商品图片
     */
    private String image;
 
    /**
     * 是否可以秒杀
     */
    private Boolean enable;
 
    /**
     * 秒杀库存
     */
    @Transient
    private Integer stock;
 
    @Transient
    private Integer seckillTotal;

}