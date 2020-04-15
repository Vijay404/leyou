package com.leyou.item.service;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import item.pojo.Sku;
import item.pojo.Spu;
import item.pojo.SpuDetail;

import java.util.List;

public interface GoodsService {
    PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key);

    void saveGoods(Spu spu);

    SpuDetail queryDetailById(Long spuId);

    List<Sku> querySkusBySpuId(Long spuId);

    void updateGoods(Spu spu);

    void deleteGoods(Long id);

    void updateSaleable(Long id ,Boolean saleable);

    Spu querySpuById(Long id);

    List<Sku> querySkusByIds(List<Long> ids);

    void decreaseStock(List<CartDTO> cartDTOS);

    Sku querySkuById(Long skuId);
}
