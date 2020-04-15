package com.leyou.seckill.web;

import com.leyou.seckill.dto.AddressDTO;
import com.leyou.seckill.dto.SeckillParams;
import com.leyou.seckill.pojo.SeckillGoods;
import com.leyou.seckill.pojo.SeckillOrder;
import com.leyou.seckill.service.SeckillOrderService;
import com.leyou.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seckill")
public class GoodsSeckillController {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private SeckillOrderService orderService;

    /**
     * 新增或者修改秒杀商品
     * @param seckillParams
     * @return
     */
    @PostMapping("/updateSecGoods")
    public ResponseEntity<Void> insertOrUpdateSeckillGoods(@RequestBody SeckillParams seckillParams){
        seckillService.insertOrUpdateSeckillGoods(seckillParams);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 查询所有秒杀商品
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<SeckillGoods>> querySeckillGoods(){
        return ResponseEntity.ok(seckillService.querySeckillGoods());
    }

    /**
     * 根据商品id，删除秒杀商品
     * @param skuId
     * @return
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteSeckillGoods(@RequestParam("id") Long skuId){
        seckillService.deleteSeckillGoods(skuId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 创建秒杀订单
     * @param addressDTO
     */
    @PostMapping("/seck")
    public ResponseEntity<Long> createSeckillOrder(@RequestBody AddressDTO addressDTO){
        return ResponseEntity.ok(orderService.createSeckillOrder(addressDTO));
    }

    /**
     * 根据订单状态查询用户的订单信息
     * @param status
     * @return
     */
    @GetMapping("/orders")
    public ResponseEntity<List<SeckillOrder>> queryOrders(
            @RequestParam(value = "status", required = false, defaultValue = "0") Integer status){
        return ResponseEntity.ok(orderService.queryOrders(status));
    }

}
