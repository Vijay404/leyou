package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import static com.github.wxpay.sdk.WXPayConstants.*; // 静态导入

import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.PayConfig;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.github.wxpay.sdk.WXPayConstants.SUCCESS;

@Slf4j
@Component
public class PayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusMapper orderStatusMapper;

    public String createOrder(Long orderId, Long totalPay, String desc){
        try {
            Map<String, String> data = new HashMap<>();
            // 商品描述
            data.put("body", desc);
            // 订单号
            data.put("out_trade_no", orderId.toString());
            //金额，单位是分
            data.put("total_fee", totalPay.toString());
            //调用微信支付的终端IP
            data.put("spbill_create_ip", "127.0.0.1");
            //回调地址
            data.put("notify_url", payConfig.getNotifyUrl());
            // 交易类型为扫码支付
            data.put("trade_type", "NATIVE");

            // 利用wxPay工具,完成下单
            Map<String, String> result = wxPay.unifiedOrder(data);

            // 判断通信和业务标示
            isSuccess(result);

            // 校验签名
            isValidSign(result);

            // 下单成功，获取支付链接
            String url = result.get("code_url");
            return url;
        } catch (Exception e) {
            log.error("[微信下单] 创建预交易订单异常失败", e);
            return null;
        }
    }

    public void isSuccess(Map<String, String> result) {
        // 判断通信标示
        String returnCode = result.get("return_code");
        if(FAIL.equals(returnCode)){
            // 通信失败
            log.error("[微信下单] 微信下单通信失败,失败原因:{}", result.get("return_msg"));
            throw new LyException(LyRespStatus.WX_PAY_ORDER_FAIL);
        }

        // 判断业务标示
        String resultCode = result.get("result_code");
        if(FAIL.equals(resultCode)){
            // 通信失败
            log.error("[微信下单] 微信下单业务失败,错误码:{}, 错误原因:{}",
                    result.get("err_code"), result.get("err_code_des"));
            throw new LyException(LyRespStatus.WX_PAY_ORDER_FAIL);
        }
    }

    public void isValidSign(Map<String, String> result) {
        // 校验签名
        try {
            String sign1 = WXPayUtil.generateSignature(result, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            String sign2 = WXPayUtil.generateSignature(result, payConfig.getKey(), WXPayConstants.SignType.MD5);

            String sign = result.get("sign");
            if (!StringUtils.equals(sign, sign1) && !StringUtils.equals(sign, sign2)) {
                throw new LyException(LyRespStatus.INVALID_SIGN_ERROR);
            }
        } catch (Exception e) {
            log.error("[微信支付] 校验签名失败，数据：{}", result);
            throw new LyException(LyRespStatus.INVALID_SIGN_ERROR);
        }
    }

    public PayState queryPayState(Long orderId) {

        try {
            Map<String, String> data = new HashMap<>();
            // 订单号
            data.put("out_trade_no", orderId.toString());
            // 查询状态
            Map<String, String> result = wxPay.orderQuery(data);

            // 校验状态
            isSuccess(result);

            // 校验签名
            isValidSign(result);

            // 校验金额
            String totalFeeStr = result.get("total_fee");
            String tradeNo = result.get("out_trade_no");
            if(StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)){
                throw new LyException(LyRespStatus.INVALID_ORDER_PARAM);
            }
            // 3.1 获取结果中的金额
            Long totalFee = Long.valueOf(totalFeeStr);
            // 3.2 获取订单金额
            Order order = orderMapper.selectByPrimaryKey(orderId);
            if(totalFee != /*order.getActualPay()*/ 1){
                // 金额不符
                throw new LyException(LyRespStatus.INVALID_ORDER_PARAM);
            }

            String state = result.get("trade_state");
            if(SUCCESS.equals(state)){
                // 支付成功
                // 修改订单状态
                OrderStatus orderStatus = new OrderStatus();
                orderStatus.setStatus(OrderStatusEnum.PAYED.status());
                orderStatus.setOrderId(orderId);
                orderStatus.setPaymentTime(new Date());
                int count = orderStatusMapper.updateByPrimaryKeySelective(orderStatus);
                if(count != 1){
                    throw new LyException(LyRespStatus.UPDATE_ORDER_STATUS_ERROR);
                }
                // 返回成功
                return PayState.SUCCESS;
            }

            if("NOTPAY".equals(state) || "USERPAYING".equals(state)){
                return PayState.NOT_PAY;
            }

            return PayState.FAIL;

        }catch (Exception e){
            return PayState.NOT_PAY;// 并不知道是否支付，再去发起请求申请一次
        }
    }

    public Map<String, String> cancelOrder(Long orderId, Long refundId, String refundDesc) {
        Map<String, String> data = new HashMap<>();
        // 订单号
        data.put("out_trade_no", orderId.toString());
        // 退款单号
        data.put("out_refund_no", refundId.toString()); // 商户退款单号
        data.put("refund_desc", refundDesc); // 退款原因
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(order == null){
            throw new LyException(LyRespStatus.ORDER_NOT_FOUND);
        }
        data.put("total_fee", order.getActualPay().toString());
        data.put("refund_fee", order.getActualPay().toString());
        try {
            Map<String, String> wxResponse = wxPay.refund(data);
            // 判断通信和业务标示
            isSuccess(wxResponse);

            // 校验签名
            isValidSign(wxResponse);
            // 退款成功
            return wxResponse;
        } catch (Exception e) {
            log.error("[微信退款] 退款失败，订单号:{}", orderId);
            throw new LyException(LyRespStatus.WX_PAY_REFUND_ERROR);
        }
    }
}