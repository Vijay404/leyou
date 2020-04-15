package com.leyou.order.service;

import com.leyou.common.vo.PageResult;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.PayState;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Long createOrder(OrderDTO orderDTO);

    List<Order> queryOrders(Integer status);

    Order queryOrderByOrderId(Long orderId);

    String createOrderPayUrl(Long orderId);

    void handleNotify(Map<String, String> wxResponse);

    PayState queryOrderStatus(Long orderId);

    OrderStatus cancelOrder(Long orderId, String desc);
}
