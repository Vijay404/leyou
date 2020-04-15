package com.leyou.seckill.service;

import com.leyou.seckill.dto.SeckillParams;
import com.leyou.seckill.pojo.SeckillGoods;

import java.util.List;

public interface SeckillService {
    List<SeckillGoods> queryAllSeckillGoods();

    List<SeckillGoods> querySeckillGoods();

    void insertOrUpdateSeckillGoods(SeckillParams seckillParams);



    void deleteSeckillGoods(Long skuId);


}
