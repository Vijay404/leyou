package com.leyou.order.web;

import com.leyou.common.vo.PageResult;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.UserAddress;
import com.leyou.order.service.AddressService;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AddressService addressService;

    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody OrderDTO orderDTO){
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    @GetMapping("/address")
    public ResponseEntity<List<UserAddress>> queryAddress(){
        return ResponseEntity.ok(addressService.queryAddress());
    }

    /**
     * 新增收货地址
     * @param address
     * @return
     */
    @PostMapping("/addAddress")
    public ResponseEntity<Void> createAddress(@RequestBody UserAddress address){
        addressService.createAddress(address);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 分页查询订单信息
     * @param status
//     * @param page
//     * @param rows
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<Order>> queryOrders(
            @RequestParam(value = "status",required = false,defaultValue = "0") Integer status
//            @RequestParam(value = "page",required = false,defaultValue = "1") Integer page,
//            @RequestParam(value = "rows",required = false,defaultValue = "5") Integer rows
    ){
        return ResponseEntity.ok(orderService.queryOrders(status));
    }

    /**
     * 根据订单id查询订单信息
     * @param orderId
     * @return
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> queryOrderByOrderId(@PathVariable("id") Long orderId){
        return ResponseEntity.ok(orderService.queryOrderByOrderId(orderId));
    }

    /**
     * 创建支付链接
     * @param orderId
     * @return
     */
    @GetMapping("/url/{id}")
    public ResponseEntity<String> createOrderPayUrl(@PathVariable("id") Long orderId){
        return ResponseEntity.ok(orderService.createOrderPayUrl(orderId));
    }

    /**
     * 根据订单id，查询订单的状态码
     * @param orderId
     * @return
     */
    @GetMapping("/state/{id}")
    public ResponseEntity<Integer> queryOrderStatus(@PathVariable("id") Long orderId){
        return ResponseEntity.ok(orderService.queryOrderStatus(orderId).getValue());
    }

    /**
     * 根据订单编号取消订单
     * @param orderId
     * @return
     */
    @GetMapping("/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestParam("id") Long orderId,
            @RequestParam(value = "desc",required = false,defaultValue = "") String desc) {
        orderService.cancelOrder(orderId, desc);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
