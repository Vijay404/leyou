package com.leyou.cart.service.impl;

import com.leyou.cart.interceptor.UserInterceptor;
import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private static final String KEY_PREFIX = "cart:uid:";

    /**
     * 新增购物车
     * @param cart
     */
    @Override
    public void addCart(Cart cart) {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        // 判断购物车是否存在
        // 是，修改数量
        // 否，新增
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        String hashKey = cart.getSkuId().toString();
        // 记录num
        Integer num = cart.getNum();
        if(ops.hasKey(hashKey)){
            // 存在，修改数量
            String jsonCart = ops.get(hashKey).toString();
            cart = JsonUtils.parse(jsonCart, Cart.class);
            cart.setNum(cart.getNum() + num);
        }
        // 写回redis
        ops.put(hashKey, JsonUtils.serialize(cart));
    }

    /**
     * 查询购物车
     * @return
     */
    @Override
    public List<Cart> queryCartList() {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        if(!redisTemplate.hasKey(key)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 查询购物车
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        List<Cart> carts = ops.values().stream()
                .map(o -> JsonUtils.parse(o.toString(), Cart.class))
                .collect(Collectors.toList());
        return carts;
    }

    /**
     * 更新购物车商品数量
     * @param skuId
     * @param num
     */
    @Override
    public void updateCartNum(String skuId, Integer num) {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        if(!redisTemplate.hasKey(key)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 查询购物车
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        // 判断购物车中是否存在该商品
        if(!ops.hasKey(skuId)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 查出该商品
        Cart cacheCart = JsonUtils.parse(ops.get(skuId).toString(), Cart.class);
        // 修改数量
        cacheCart.setNum(num);
        // 写回redis
        ops.put(skuId, JsonUtils.serialize(cacheCart));
    }

    /**
     *
     * @param skuId
     */
    @Override
    public void deleteCart(String skuId) {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        if(!redisTemplate.hasKey(key)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 查询购物车
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        // 判断购物车中是否存在该商品
        if(!ops.hasKey(skuId)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 删除商品
        ops.delete(skuId);
    }

    /**
     * 根据多个商品id，删除购物车中的商品
     * @param ids
     * @return
     */
    @Override
    public void deleteCartByIds(List<String> ids) {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        if(!redisTemplate.hasKey(key)){
            throw new LyException(LyRespStatus.CART_NOT_FOUND);
        }
        // 查询购物车
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        // 判断购物车中是否存在该商品
        for (String id : ids) {
            // 判断购物车中是否存在该商品
            if(!ops.hasKey(id)){
                throw new LyException(LyRespStatus.CART_NOT_FOUND);
            }
            // 删除商品
            ops.delete(id);
        }
    }

    /**
     * 合并本地购物车
     * @param carts
     */
    @Override
    public void mergeCarts(List<Cart> carts) {
        // 获取登录的用户
        UserInfo user = UserInterceptor.getUser();
        String key =KEY_PREFIX + user.getId();
        BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(key);
        // 存在购物车，批量新增购物车商品
        Map<String, Cart> cartMap = carts.stream()
                .collect(Collectors.toMap(c -> c.getSkuId().toString(),
                        cart -> cart));
        // 获取carts的key集合
        List<String> skuIds = carts.stream().map(c -> c.getSkuId().toString()).collect(Collectors.toList());
        // 判断购物车中是否已存在商品
        for (String skuId : skuIds) {
            if(ops.hasKey(skuId)){
                // 存在，修改数量即可
                String jsonCart = ops.get(skuId).toString();
                Cart cart = JsonUtils.parse(jsonCart, Cart.class);
                cart.setNum(cart.getNum() + cartMap.get(skuId).getNum());
                ops.put(skuId, JsonUtils.serialize(cart));
                // 从map中移除已新增的商品
                cartMap.remove(skuId);
            }
        }
        // 购物车中不存在该商品，直接批量新增，先转化类型
        Map<String, String> map = new HashMap<>();
        Set<Map.Entry<String, Cart>> entries = cartMap.entrySet();
        for (Map.Entry<String, Cart> entry : entries) {
            map.put(entry.getKey(), JsonUtils.serialize(entry.getValue()));
        }
        ops.putAll(map);
    }
}
