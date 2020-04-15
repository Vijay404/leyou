package com.leyou.order.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.pojo.UserInfo;
import com.leyou.common.utils.IdWorker;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.client.SpecificationClient;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.mapper.UserAddressMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.pojo.UserAddress;
import com.leyou.order.service.OrderService;
import com.leyou.order.utils.PayHelper;
import item.pojo.Sku;
import item.pojo.SpecParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private UserAddressMapper addressMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private PayHelper payHelper;
    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    @Override
    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        int count = 0;
        // 1 新增订单
        Order order = new Order();
        // 1.1 订单编号，基本信息
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        // 初始化order字段
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 1.2 用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        // 1.3 收货人地址信息
        UserAddress address = addressMapper.selectByPrimaryKey(orderDTO.getAddressId());
        if(address == null){
            throw new LyException(LyRespStatus.ADDRESS_NOT_FOUND);
        }
        order.setReceiver(address.getName());
        order.setReceiverAddress(address.getAddress());
        order.setReceiverCity(address.getCity());
        order.setReceiverDistrict(address.getDistrict());
        order.setReceiverMobile(address.getPhone());
        order.setReceiverState(address.getState());
        order.setReceiverZip(address.getZipCode());
        // 1.4 金额
        // 把CartDTO的list集合转为map集合，key是skuId，值是商品数量
        Map<Long, Integer> numMap = orderDTO.getCarts().stream()
                .collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        // 获取所有的skuId
        Set<Long> idSet = numMap.keySet();
        // 根据商品id查询所有的sku
        List<Sku> skus = goodsClient.querySkusByIds(new ArrayList<>(idSet));
        // 计算价格
        // 准备OrderDetail集合
        List<OrderDetail> details = new ArrayList<>();

        long totalPrice = 0L;
        for (Sku sku : skus) {
            totalPrice += sku.getPrice() * numMap.get(sku.getId());
            // 封装OrderDetail
            OrderDetail detail = new OrderDetail();
            detail.setSkuId(sku.getId());
            // 取到一张图片
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setNum(numMap.get(sku.getId()));
            detail.setOrderId(orderId);
            String ownSpec = sku.getOwnSpec();
            // 处理ownSpec:"{"4":"白色","12":"3GB","13":"16GB"}"，将参数id转为参数名称
            String newSpec = handleOwnSpec(ownSpec);
            detail.setOwnSpec(newSpec);
            detail.setPrice(sku.getPrice());
            detail.setTitle(sku.getTitle());
            // 放入集合
            details.add(detail);
        }
        order.setTotalPay(totalPrice);
        // 实付金额： 总金额 + 邮费 - 优惠金额
        order.setActualPay(totalPrice + order.getPostFee() - 0);
        // 写入数据库
        count = orderMapper.insertSelective(order);
        if(count == 0){
            log.error("[创建订单] 创建订单失败，orderId:{}", orderId);
            throw new LyException(LyRespStatus.ORDER_SAVE_ERROR);
        }

        // 2 新增订单详情
        count = detailMapper.insertList(details);
        if(count == 0){
            log.error("[创建订单] 创建订单失败，orderId:{}", orderId);
            throw new LyException(LyRespStatus.ORDER_SAVE_ERROR);
        }
        // 3 新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setStatus(OrderStatusEnum.NO_PAY.status());
        count = statusMapper.insert(orderStatus);
        if(count == 0){
            log.error("[创建订单] 创建订单失败，orderId:{}", orderId);
            throw new LyException(LyRespStatus.ORDER_SAVE_ERROR);
        }
        // 4 减库存
        List<CartDTO> carts = orderDTO.getCarts();
        goodsClient.decreaseStock(carts);

        return orderId;
    }

    /**
     * 将ownSpec中的参数id转为参数名称
     * @param ownSpec
     * @return
     */
    private String handleOwnSpec(String ownSpec) {
        StringBuilder sb = new StringBuilder(ownSpec);
        // ownSpec:"{"4":"白色","12":"3GB","13":"16GB"}"，去除"{"和"}"
        String substring = sb.substring(1, sb.length() - 1);// "4":"白色","12":"3GB","13":"16GB"

//        String[] ownSpec1 = StringUtils.split(ownSpec, "{"); // split效率很低
//        String[] ownSpec2 = StringUtils.split(ownSpec1[0], "}");

        // ""4":"白色","12":"3GB","13":"16GB""
        // 拆封字符串，拿到参数id，""4":"白色""、""12":"3GB""、""13":"16GB"" sb
        String[] s1 = StringUtils.split(substring, ","); // "4":"白色"
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < s1.length; i++) {
            // 取出编号
            String[] s2 = StringUtils.split(s1[i], ":"); // ""4""和""白色""
            ids.add(Long.valueOf(JsonUtils.serialize(StringUtils.split(s2[0], "\"")[0]))); // s2[0]: ""4""
        }
        // 根据参数id集合查询参数名称
        List<SpecParam> params = specClient.queryParamByIds(ids);
        // 将规格参数id作为key，参数名称作为value
        Map<Long, String> paramMap = params.stream()
                .collect(Collectors.toMap(SpecParam::getId, SpecParam::getName));
        List<String> newSpec = new ArrayList<>();
        for (int i = 0; i < s1.length; i++) {
            String[] s2 = StringUtils.split(s1[i], ":");
            String name = paramMap.get(Long.valueOf(JsonUtils.serialize(StringUtils.split(s2[0], "\"")[0])));
            newSpec.add("\""+ name + "\"" + ":" + JsonUtils.serialize(s2[1]));
        }
        // 去除List转String的"["和"]"
        sb = new StringBuilder(newSpec.toString());
        String s2 = sb.substring(1, sb.length() - 1);

//        String s = StringUtils.split(newSpec.toString(), "[")[0];
//        String s2 = StringUtils.split(s, "]")[0];
        return "{" + s2 + "}";
    }

    /**
     * 分页查询订单信息
     * @param status
//     * @param page
//     * @param rows
     * @return
     */
    @Override
    public List<Order> queryOrders(Integer status) {
        // 获取当前登录的用户
        UserInfo user = UserInterceptor.getUser();

        // 分页，订单不需要分页
//        PageHelper.startPage(page, rows);
        // 根据用户id查询出订单
        Order order = new Order();
        order.setUserId(user.getId());
        List<Order> orders = orderMapper.select(order);
        if(CollectionUtils.isEmpty(orders)){
            throw new LyException(LyRespStatus.ORDER_NOT_FOUND);
        }
        // 生成订单id集合
        List<Long> ids = orders.stream().map(Order::getOrderId).collect(Collectors.toList());
        if(status != 0){ // 根据状态码，分类处理信息
            // 根据每个订单id查询出所有的订单状态信息
            List<OrderStatus> statusList = statusMapper.selectByIdList(ids);
            // 处理statusList为map，将订单id作为key，status字段作为值
            Map<Long, Integer> statusMap = statusList.stream()
                    .collect(Collectors.toMap(OrderStatus::getOrderId, OrderStatus::getStatus));
            Iterator<Order> it = orders.iterator();
            while(it.hasNext()){
                Order o = it.next();
                // 根据订单id，取出订单状态码
                Integer dbStatus = statusMap.get(o.getOrderId());
                if(dbStatus == null){
                    log.error("[订单服务] 用户订单状态异常，订单id:{}", o.getOrderId());
                    throw new LyException(LyRespStatus.ORDER_ERROR);
                }
                // 比较状态码是否一致
                if(!status.equals(dbStatus)){
                    // 不一致，将当前订单取出集合
                    it.remove();
                }
            }
        }
        // 状态码0，查询出用户的所有订单信息
        if (CollectionUtils.isEmpty(orders)) {
            throw new LyException(LyRespStatus.ORDER_NOT_FOUND);
        }
        // 查询订单详情、订单状态信息并封装
        for (Order o : orders) {
            // 根据订单id查询订单详情
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(o.getOrderId());
            List<OrderDetail> details = detailMapper.select(detail);
            o.setOrderDetails(details);
            // 根据订单id查询订单状态信息
            OrderStatus orderStatus = statusMapper.selectByPrimaryKey(o.getOrderId());
            if(orderStatus == null){
                throw new LyException(LyRespStatus.ORDER_STATUS_NOT_FOUND);
            }
            o.setOrderStatus(orderStatus);
        }
        // 生成分页对象
//        PageInfo<Order> pageInfo = new PageInfo<>(orders);
        return orders;
    }

    /**
     * 根据订单id查询订单信息
     * @param orderId
     * @return
     */
    @Override
    public Order queryOrderByOrderId(Long orderId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(order == null){
            throw new LyException(LyRespStatus.ORDER_NOT_FOUND);
        }
        // 根据订单id查询订单状态
        OrderStatus status = statusMapper.selectByPrimaryKey(orderId);
        if(status == null){
            throw new LyException(LyRespStatus.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(status);
        // 查询订单详情
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> details = detailMapper.select(orderDetail);
        if(CollectionUtils.isEmpty(details)){
            throw new LyException(LyRespStatus.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);
        return order;
    }

    /**
     * 创建订单支付链接
     * @param orderId
     * @return
     */
    @Override
    public String createOrderPayUrl(Long orderId) {
        Order order = queryOrderByOrderId(orderId);
        // 判断订单状态
        if(order.getOrderStatus().getStatus() != OrderStatusEnum.NO_PAY.status()){
            throw new LyException(LyRespStatus.ORDER_STATUS_ERROR);
        }
        // 商品描述
        OrderDetail detail = order.getOrderDetails().get(0);
        return payHelper.createOrder(orderId, 1L, detail.getTitle());
    }

    /**
     * 微信支付回调
     * @param wxResponse
     */
    @Override
    @Transactional
    public void handleNotify(Map<String, String> wxResponse) {
        // 数据校验，判断通信和业务标示
        payHelper.isSuccess(wxResponse);

        // 校验签名
        payHelper.isValidSign(wxResponse);

        // 3 金额校验
        String totalFeeStr = wxResponse.get("total_fee");
        String tradeNo = wxResponse.get("out_trade_no");
        if(StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)){
            throw new LyException(LyRespStatus.INVALID_ORDER_PARAM);
        }
        Long totalFee = Long.valueOf(totalFeeStr);
        Long orderId = Long.valueOf(tradeNo);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(totalFee != /*order.getActualPay()*/ 1){
            // 金额不符
            throw new LyException(LyRespStatus.INVALID_ORDER_PARAM);
        }

        // 4 修改订单状态
        OrderStatus status = new OrderStatus();
        status.setStatus(OrderStatusEnum.PAYED.status());
        status.setOrderId(orderId);
        status.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(status);
        if(count != 1){
            throw new LyException(LyRespStatus.UPDATE_ORDER_STATUS_ERROR);
        }

        log.info("[订单回调], 订单支付成功! 订单编号:{}", orderId);
    }

    /**
     * 根据订单id，查询订单的状态码
     * @param orderId
     * @return
     */
    @Override
    public PayState queryOrderStatus(Long orderId) {
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if(orderStatus == null){
            log.error("[订单支付服务] 订单状态错误，订单号:{}", orderId);
            throw new LyException(LyRespStatus.ORDER_STATUS_NOT_FOUND);
        }
        Integer status = orderStatus.getStatus();
        // 判断是否支付
        if(status != OrderStatusEnum.NO_PAY.status()){
            // 已支付，返回支付成功
            return PayState.SUCCESS;
        }
        // 数据库查询到未支付，重新去微信支付校验支付状态
        // 订单支付有延迟，有可能已支付，但是微信回调接口没有来及发生，所以不能直接从数据库做比对，而是重新去微信校验
        return payHelper.queryPayState(orderId);
    }

    /**
     * 根据订单编号取消订单
     * @param orderId
     * @param desc
     * @return
     */
    @Override
    @Transactional
    public OrderStatus cancelOrder(Long orderId, String desc) {
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if(orderStatus == null){
            throw new LyException(LyRespStatus.ORDER_STATUS_NOT_FOUND);
        }
        if(orderStatus.getStatus() == OrderStatusEnum.NO_PAY.status()){
            // 未付款，直接取消订单
            closeOrder(orderStatus);
        }
        if(orderStatus.getStatus() == OrderStatusEnum.PAYED.status()){
            // 已付款，未发货，可取消订单
            // 调用微信接口，发起退款
            // 生成退款单号
            long refundId = idWorker.nextId();
            payHelper.cancelOrder(orderId, refundId, desc);
            closeOrder(orderStatus);
        }

        return orderStatus;
    }

    private void closeOrder(OrderStatus orderStatus) {
        orderStatus.setStatus(OrderStatusEnum.CLOSED.status());
        orderStatus.setCloseTime(new Date());
        // 更新数据
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1) {
            throw new LyException(LyRespStatus.UPDATE_ORDER_STATUS_ERROR);
        }
    }
}
