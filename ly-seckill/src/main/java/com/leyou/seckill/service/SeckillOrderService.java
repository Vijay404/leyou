package com.leyou.seckill.service;

import com.leyou.seckill.dto.AddressDTO;
import com.leyou.seckill.pojo.SeckillOrder;

import java.util.List;

public interface SeckillOrderService {
    Long createSeckillOrder(AddressDTO addressDTO);

    List<SeckillOrder> queryOrders(Integer status);
}
