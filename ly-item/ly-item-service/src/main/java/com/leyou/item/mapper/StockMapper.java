package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import item.pojo.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

public interface StockMapper extends BaseMapper<Stock,Long> {

    @Update("update tb_stock set stock = stock - #{stock} where sku_id = #{skuId} and stock >= #{stock}")
    int updateStock(@Param("stock") Integer stock,@Param("skuId") Long skuId);
}
