package com.leyou.cart.service;

import com.leyou.cart.pojo.Cart;

import java.util.List;

public interface CartService {
    void addCart(Cart cart);

    List<Cart> queryCartList();

    void updateCartNum(String skuId, Integer num);

    void deleteCart(String skuId);

    void deleteCartByIds(List<String> ids);

    void mergeCarts(List<Cart> carts);
}
