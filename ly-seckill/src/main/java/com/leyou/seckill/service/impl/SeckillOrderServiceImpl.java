package com.leyou.seckill.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.IdWorker;
import com.leyou.seckill.annotation.AccessForSeckill;
import com.leyou.seckill.dto.AddressDTO;
import com.leyou.seckill.enums.OrderStatusEnum;
import com.leyou.seckill.interceptor.UserInterceptor;
import com.leyou.seckill.mapper.SeckillOrderMapper;
import com.leyou.seckill.mapper.StockMapper;
import com.leyou.seckill.pojo.SeckillGoods;
import com.leyou.seckill.pojo.SeckillOrder;
import com.leyou.seckill.pojo.Stock;
import com.leyou.seckill.service.SeckillOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private SeckillOrderMapper orderMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    
    private static final String SECKILL_GOODS_KEY = "seckill:goods:";

    private static final String SECKILL_ORDER_KEY = "seckill:order:";

    private static final String USER_PREFIX = "user:";

    /**
     * 创建秒杀订单，返回订单号
     * @param addressDTO
     */
    @Override
    @Transactional
    @AccessForSeckill(login = true)
    public Long createSeckillOrder(AddressDTO addressDTO) {
        // 校验token，并获取当前登录用户
        UserInfo user = UserInterceptor.getUser();
        try {
            //开启redis事务支持
            Long skuId = addressDTO.getSkuId();

            SeckillOrder order = new SeckillOrder();
            // 查询redis中的秒杀商品
            // 判断库存，使用乐观锁
            BoundHashOperations ops = redisTemplate.boundHashOps(SECKILL_GOODS_KEY);
            BoundHashOperations orderOps = redisTemplate.boundHashOps(SECKILL_ORDER_KEY);

            SeckillGoods seckillGoods = (SeckillGoods) ops.get(skuId);
            if(seckillGoods == null){
                throw new LyException(LyRespStatus.SECKILL_SKU_NOT_FOUND);
            }
            Integer stock = seckillGoods.getStock();
            Integer skuStock = (Integer)redisTemplate.opsForValue().get(skuId);
            Integer totalStock = seckillGoods.getSeckillTotal();
            if(stock <= 0 || skuStock == null){
                // 无库存，已秒杀完成，直接返回，更新数据库库存
                Stock dbStock = new Stock();
                dbStock.setSkuId(skuId);
                dbStock.setSeckillStock(totalStock);
                int count = stockMapper.updateByPrimaryKeySelective(dbStock);
                if(count != 1){
                    throw new LyException(LyRespStatus.GOODS_STOCK_UPDATE_ERROR);
                }
                // 从redis中移除商品
                redisTemplate.boundHashOps(SECKILL_GOODS_KEY).delete(skuId);
                log.error("[秒杀订单] 商品库存不足，秒杀失败，商品编号：{}", skuId);
                throw new LyException(LyRespStatus.SECKILL_GOODS_STOCK_LOW);
            }
            // 有库存，创建订单，且为当前秒杀商品的库存加锁
            SessionCallback<Object> callback = new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.watch(skuId);
                    operations.multi(); // 开启事务
                    // 防止用户重复提交订单
                    /**
                     * 当setIfAbsent()方法在开始事务时，返回值为null
                     */
                    operations.opsForValue().setIfAbsent(USER_PREFIX + user.getId(), "1");
                    seckillGoods.setStock(stock - 1);
                    operations.opsForValue().set(skuId, (skuStock - 1));
                    ops.put(skuId, seckillGoods);
                    return operations.exec();
                }
            };
            List result = (List)redisTemplate.execute(callback);
            // 判断用户标识，setIfAbsent(user.getId(), "1")，防止重复提交订单
            if((Boolean) result.get(0)){
                throw new LyException(LyRespStatus.SECKILL_ORDER_EXISTS);
            }
            if(result == null){
                // 库存被其他订单修改，不提交订单，返回抢购失败
                throw new LyException(LyRespStatus.SECKILL_GOODS_STOCK_LOW);
            }
            // 减库存成功，创建订单，存入redis，当付款后再存入数据库
            // 生成订单号
            Long orderId = idWorker.nextId();
            order.setId(orderId);
            order.setUserId(user.getId());
            order.setCreateTime(new Date());
            order.setPayTime(null);
            order.setSkuId(skuId);
            order.setPrice(seckillGoods.getSeckillPrice());
            order.setStatus(OrderStatusEnum.NO_PAY.status());
            order.setReceiver(addressDTO.getReceiver()); // 收件人信息，在付款之后存入数据库中
            order.setAddress(addressDTO.getAddress());
            order.setPhone(addressDTO.getPhone());
            // 订单存入redis
            orderOps.put(user.getId(), order); // 以用户id为键，值为SeckillOrder对象
            return orderId;
        }finally {
            // 不管订单有没有创建成功，都清除redis标记
            redisTemplate.delete(USER_PREFIX + user.getId());
        }
    }

    /**
     * 根据订单状态查询用户的订单信息
     * @param status
     * @return
     */
    @Override
    public List<SeckillOrder> queryOrders(Integer status) {
        // 校验并获取当前登录的用户
        UserInfo user = UserInterceptor.getUser();
        Long userId = user.getId();
        // 判断订单状态
        if(status.equals(0)){
            // 查询全部订单，从redis中查
            BoundHashOperations ops = redisTemplate.boundHashOps(SECKILL_ORDER_KEY);
            List<SeckillOrder> orders = ops.values();
            // 从数据库查
            SeckillOrder order = new SeckillOrder();
            order.setUserId(userId);
            List<SeckillOrder> dbOrders = orderMapper.select(order);
            if(CollectionUtils.isEmpty(dbOrders) && CollectionUtils.isEmpty(orders)){
                throw new LyException(LyRespStatus.SECKILL_ORDER_NOT_FOUND);
            }
            dbOrders.addAll(orders); // 取两个结果的并集
            return dbOrders;
        }
        if(status.equals(OrderStatusEnum.NO_PAY.status())){
            // 未付款订单，从redis中查询
            BoundHashOperations ops = redisTemplate.boundHashOps(SECKILL_ORDER_KEY);
            List<SeckillOrder> orders = ops.values();
            if(CollectionUtils.isEmpty(orders)){
                throw new LyException(LyRespStatus.SECKILL_ORDER_NOT_FOUND);
            }
            return orders;
        }
        // 其他状态则从数据库中查询
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        List<SeckillOrder> orders = orderMapper.select(order);
        if(CollectionUtils.isEmpty(orders)){
            throw new LyException(LyRespStatus.SECKILL_ORDER_NOT_FOUND);
        }
        return orders;
    }
}
