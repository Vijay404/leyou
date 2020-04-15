package com.leyou.seckill.dto;

import lombok.Data;
import java.util.Date;

/**
 * dto：DataTransferObject
 * 用来接收前端表单数据
 */
@Data
public class SeckillParams {
 
    /**
     * 要秒杀的sku id
     */
    private Long id;
 
    /**
     * 秒杀开始时间
     */
    private Date startTime;
 
    /**
     * 秒杀结束时间
     */
    private Date endTime;
 
    /**
     * 参与秒杀的商品数量
     */
    private Integer count;

    /**
     * 秒杀商品总量
     */
    private Integer totalCount;

    /**
     * 折扣
     */
    private double  discount;

    /**
     * 折扣
     */
    private Boolean  enable;
 
}