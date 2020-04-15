package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum  LyRespStatus {
    INVALID_USER_DATA_TYPE(400,"用户数据类型无效！"),
    USER_REGISTER_ERROR(500,"用户创建失败！"),
    INVALID_CODE(400,"验证码失效或参数有误！"),
    USER_NOT_EXIST(400,"用户不存在！"),
    PASSWORD_MISMATCH(400,"密码错误！"),
    UNAUTHORIZED(401,"身份未验证！"),
    PRICE_CANNOT_BE_BULL(400,"价格不能为空！"),//定义一个枚举对象
    GOODS_CANNOT_BE_BULL(400,"商品id不能为空！"),
    CATEGORY_NOT_FOUND(404,"商品分类不存在"),
    GOODS_NOT_FOUND(404,"商品不存在"),
    GOODS_DETAIL_NOT_FOUND(404,"商品详情不存在"),
    GOODS_SKU_NOT_FOUND(404,"商品SKU不存在"),
    GOODS_STOCK_NOT_FOUND(404,"商品库存不存在"),
    GOODS_STOCK_UPDATE_ERROR(400,"修改商品库存失败"),
    GOODS_STOCK_LOW(400,"商品库存不足！"),
    SECKILL_GOODS_STOCK_LOW(400,"秒杀商品库存不足！"),
    SPEC_GROUP_NOT_FOUND(404,"商品规格组不存在"),
    SPEC_PARAM_NOT_FOUND(404,"商品规格参数不存在"),
    BRAND_NOT_FOUND(404,"品牌不存在"),
    CATEGORY_SAVE_ERROR(500,"新增分类失败"),
    CATEGORY_UPDATE_ERROR(500,"修改分类失败"),
    CATEGORY_DELETE_ERROR(500,"删除分类失败"),
    BRAND_SAVE_ERROR(500,"新增品牌失败"),
    BRAND_UPDATE_ERROR(500,"修改品牌失败"),
    BRAND_DELETE_ERROR(500,"删除品牌失败"),
    SPEC_GROUP_UPDATE_ERROR(500,"修改规格组失败"),
    SPEC_GROUP_DELETE_ERROR(500,"删除规格组失败"),
    SPEC_GROUP_SAVE_ERROR(500,"新增规格组失败"),
    SPEC_PARAM_UPDATE_ERROR(500,"修改规格参数失败"),
    SPEC_PARAM_SAVE_ERROR(500,"新增规格参数失败"),
    SPEC_PARAM_DELETE_ERROR(500,"删除规格参数失败"),
    GOODS_SAVE_ERROR(500,"新增商品失败"),
    GOODS_UPDATE_ERROR(500,"修改商品失败"),
    GOODS_DELETE_ERROR(500,"修改商品失败"),
    UPLOAD_FILE_ERROR(500,"文件上传失败"),
    INVALID_FILE_TYPE(400,"无效的文件类型"),
    CART_NOT_FOUND(404,"购物车不存在"),
    ADDRESS_NOT_FOUND(404,"用户地址不存在"),
    ADDRESS_SAVE_ERROR(500,"新增地址失败！"),
    ORDER_SAVE_ERROR(500,"新增订单失败！"),
    WX_PAY_REFUND_ERROR(500,"微信退款失败！"),
    ORDER_NOT_FOUND(404,"用户订单为空！"),
    ORDER_DETAIL_NOT_FOUND(404,"用户订单详情为空！"),
    ORDER_STATUS_NOT_FOUND(404,"用户订单状态为空！"),
    ORDER_ERROR(500,"用户订单异常！"),
    ORDER_STATUS_ERROR(500,"用户订单状态异常！"),
    WX_PAY_ORDER_FAIL(500,"微信下单失败！"),
    INVALID_ORDER_PARAM(400,"微信支付金额不符！"),
    INVALID_SIGN_ERROR(400,"微信签名无效！"),
    UPDATE_ORDER_STATUS_ERROR(400,"修改订单状态失败！"),
    SECKILL_SKU_SAVE_ERROR(400, "新增秒杀商品失败！"),
    SECKILL_SKU_UPDATE_ERROR(400, "更新秒杀商品失败！"),
    DELETE_SECKILL_SKU_ERROR(400, "删除秒杀商品失败！"),
    SECKILL_ORDER_EXISTS(500, "请勿重复提交订单！"),
    SECKILL_ORDER_NOT_FOUND(404, "用户秒杀订单为空！"),
    CREATE_SECKILL_ORDER_ERROR(400, "创建秒杀订单失败！"),
    SECKILL_SKU_NOT_FOUND(404,"秒杀商品为空！"),
    ;
    private int code;
    private String msg;
}
