package com.leyou.cart.web;

import com.leyou.cart.pojo.Cart;
import com.leyou.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 新增购物车
     * @param cart
     * @return
     */
    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart){
        cartService.addCart(cart);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 查询购物车
     * @return
     */
    @GetMapping("list")
    public ResponseEntity<List<Cart>> queryCartList(){
        return ResponseEntity.ok(cartService.queryCartList());
    }

    /**
     * 更新购物车商品数量
     * @param skuId
     * @param num
     * @return
     */
    @PutMapping
    public ResponseEntity<Void> updateCartNum(
            @RequestParam("id") String skuId,
            @RequestParam("num") Integer num){
        cartService.updateCartNum(skuId, num);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 删除购物车中的商品，或者删除购物车
     * @param skuId
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable("id") String skuId){
        cartService.deleteCart(skuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 根据多个商品id，删除购物车中的商品
     * @param ids
     * @return
     */
    @DeleteMapping("/ids")
    public ResponseEntity<Void> deleteCartByIds(@RequestParam("ids") List<String> ids){
        cartService.deleteCartByIds(ids);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 合并购物车
     * @param carts
     * @return
     */
    @PostMapping("addList")
    public ResponseEntity<Void> mergeCarts(@RequestBody List<Cart> carts){
        cartService.mergeCarts(carts);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
