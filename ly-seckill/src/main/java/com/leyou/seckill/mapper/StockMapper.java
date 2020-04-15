package com.leyou.seckill.mapper;

import com.leyou.common.mapper.BaseMapper;
import com.leyou.seckill.pojo.Stock;
import org.apache.ibatis.annotations.Update;

public interface StockMapper extends BaseMapper<Stock, Long> {

    @Update("update tb_stock set seckill_stock = seckill_stock - 1, seckill_total = seckill_total - 1 where sku_id = #{skuId} and seckill_stock >= 1")
    int updateSeckillStock(Long skuId);
}
