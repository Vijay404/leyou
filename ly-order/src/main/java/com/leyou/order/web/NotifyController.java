package com.leyou.order.web;

import com.leyou.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notify")
public class NotifyController {

    @Autowired
    private OrderService orderService;

    /**
     * 微信支付的回调接口
     * @param wxResponse
     * @return
     */
    @PostMapping(value = "/pay", produces = "application/xml") // produces定义数据的返回类型为xml类型
    public Map<String, String> handleNotify(@RequestBody Map<String ,String> wxResponse){
        orderService.handleNotify(wxResponse);
        log.info("[微信支付回调] 微信支付成功，订单号:{}", wxResponse.get("out_trade_no"));

        // 返回成功
        Map<String, String> response = new HashMap<>();
        response.put("return_code", "SUCCESS");
        response.put("return_msg", "OK");
        return response;
    }
}




