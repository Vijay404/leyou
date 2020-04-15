package com.leyou.seckill.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.seckill.client.GoodsClient;
import com.leyou.seckill.dto.SeckillParams;
import com.leyou.seckill.mapper.SeckillMapper;
import com.leyou.seckill.mapper.StockMapper;
import com.leyou.seckill.pojo.SeckillGoods;
import com.leyou.seckill.pojo.Stock;
import com.leyou.seckill.service.SeckillService;
import item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillMapper seckillMapper;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private static final String SECKILL_GOODS_KEY = "seckill:goods:";

    /**
     * 添加或更新秒杀商品
     * @param seckillParams
     */
    @Override
    @Transactional
    public void insertOrUpdateSeckillGoods(SeckillParams seckillParams) {
        int count = 0;
        // 查询秒杀表中是否存在该秒杀商品
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setSkuId(seckillParams.getId());
        count = seckillMapper.selectCount(seckillGoods);
        // 封装数据
        seckillGoods.setStartTime(seckillParams.getStartTime());
        seckillGoods.setEndTime(seckillParams.getEndTime());
        seckillGoods.setSeckillTotal(seckillParams.getCount());

        // 根据商品id，查询出商品参数信息
        Sku sku = goodsClient.querySkuById(seckillParams.getId());
        if(sku == null){
            throw new LyException(LyRespStatus.GOODS_SKU_NOT_FOUND);
        }
        // 计算秒杀价格
        seckillGoods.setLastPrice(sku.getPrice()); // 商品原价
        seckillGoods.setSeckillPrice(sku.getPrice() * seckillParams.getDiscount());
        seckillGoods.setImage(StringUtils.split(sku.getImages(), ",")[0]);
        seckillGoods.setTitle(sku.getTitle());
        seckillGoods.setEnable(seckillParams.getEnable());
        if(count == 1){ // 已存在
            // 更新秒杀表
            count = seckillMapper.updateByPrimaryKeySelective(seckillGoods);
            if(count != 1){
                throw new LyException(LyRespStatus.SECKILL_SKU_UPDATE_ERROR);
            }
        }else { // 不存在，新增
            // 新增秒杀表
            count = seckillMapper.insertSelective(seckillGoods);
            if(count != 1){
                throw new LyException(LyRespStatus.SECKILL_SKU_SAVE_ERROR);
            }
        }
        // 查询并更新库存信息
        Stock stock = stockMapper.selectByPrimaryKey(seckillParams.getId());
        // 处理库存
        if(stock.getSeckillStock() == 0 || stock.getSeckillStock() == null){
            // 无秒杀库存
            stock.setSeckillStock(seckillParams.getCount()); // 可秒杀库存
            stock.setSeckillTotal(seckillParams.getTotalCount()); // 秒杀总数量
            stock.setStock(stock.getStock() - seckillParams.getTotalCount());
            count = stockMapper.updateByPrimaryKeySelective(stock);
            if(count != 1){
                throw new LyException(LyRespStatus.GOODS_STOCK_UPDATE_ERROR);
            }
        }else{
            // 有秒杀库存
            stock.setSeckillStock(seckillParams.getCount());
            stock.setSeckillTotal(seckillParams.getTotalCount());
            if(seckillParams.getCount() > seckillParams.getTotalCount()){ // 当前秒杀库存要比总秒杀商品量要少
                throw new LyException(LyRespStatus.GOODS_STOCK_UPDATE_ERROR);
            }
            // 最终普通商品的库存 = 上次普通商品库存量 + 秒杀商品库存变化量(就是：上次秒杀商品总库存量 - 修改后的秒杀商品总库存量)
            Integer remainStock = stock.getSeckillTotal() - seckillParams.getTotalCount() + stock.getStock();
            stock.setStock(remainStock);
            count = stockMapper.updateByPrimaryKeySelective(stock);
            if(count != 1){
                throw new LyException(LyRespStatus.GOODS_STOCK_UPDATE_ERROR);
            }
        }
        // 更新redis数据，不需要删除，直接新增就是修改
        redisTemplate.boundHashOps(SECKILL_GOODS_KEY).put(seckillGoods.getSkuId(), seckillGoods);
        // 更新redis中的秒杀商品库存信息
        redisTemplate.opsForValue().set(seckillGoods.getSkuId(), seckillGoods.getStock());
    }

    /**
     * 条件查询所有的秒杀商品
     * @return
     */
    @Override
    public List<SeckillGoods> queryAllSeckillGoods() {
        // 从数据库中查询所有秒杀商品
        List<SeckillGoods> goods = seckillMapper.selectByExample(null);
        if(CollectionUtils.isEmpty(goods)){
            throw new LyException(LyRespStatus.SECKILL_SKU_NOT_FOUND);
        }
        // 获取skuId的集合
        List<Long> ids = goods.stream().map(g -> g.getSkuId()).collect(Collectors.toList());
        // 根据ids查出所有库存信息
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        // 封装库存信息
        handleStock(goods, stocks);
        return goods;
    }

    /**
     * 条件查询所有可秒杀商品
     * @return
     */
    @Override
//    @AccessForSeckill(login = true)
    public List<SeckillGoods> querySeckillGoods() {
        List<SeckillGoods> goods = redisTemplate.boundHashOps(SECKILL_GOODS_KEY).values();
        // 判断redis中是否存在秒杀商品缓存
        if (!CollectionUtils.isEmpty(goods)) {
            // 存在，直接从redis中查询
            // 获取skuId的集合
            List<Long> ids = goods.stream().map(g -> g.getSkuId()).collect(Collectors.toList());
            // 根据ids查出所有库存信息
            List<Stock> stocks = stockMapper.selectByIdList(ids);
            // 封装库存信息
            handleStock(goods, stocks);
            System.out.println("从redis中查询");
            return goods;
        }else{
            System.out.println("从数据库中查询");
        }

        // redis中不存在，从数据库中根据是否可秒杀，查询秒杀商品
        Example example = new Example(SeckillGoods.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("enable", true); // 参与秒杀的商品
        criteria.andLessThanOrEqualTo("startTime", new Date()); // 开始秒杀的时间小于等于当前时间
        criteria.andGreaterThanOrEqualTo("endTime", new Date()); // 结束秒杀时间要比当前时间相同或更晚，才可秒杀
        goods = seckillMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(goods)){
            throw new LyException(LyRespStatus.SECKILL_SKU_NOT_FOUND);
        }
        // 验证库存信息，库存为0则不可秒杀
        List<Long> ids = goods.stream().map(SeckillGoods::getSkuId).collect(Collectors.toList());
        List<Stock> stocks = stockMapper.selectByIdList(ids);
        for (Stock stock : stocks) {
            // 判断秒杀库存是否大于0
            if(stock.getSeckillStock() <= 0){
                // 无库存，从goods中移除
                Iterator<SeckillGoods> it = goods.iterator();
                while (it.hasNext()){
                    if(stock.getSkuId().equals(it.next().getSkuId())){
                        // 同一个商品，移除
                        it.remove();
                    }
                }
            }
        }

        // 封装库存信息
        handleStock(goods, stocks);

        // 可秒杀商品，存入redis中缓存
        // 将商品处理为键为商品id，值为SeckillGoods本身
//        Map<String, SeckillGoods> goodsMap = goods.stream().collect(
//                Collectors.toMap(s -> s.getSkuId().toString(), Function.identity()));
//        // 处理为String结构map
//        Map<String, String> map = new HashMap<>();
//        Set<Map.Entry<String, SeckillGoods>> entries = goodsMap.entrySet();
//        for (Map.Entry<String, SeckillGoods> entry : entries) {
//            map.put(entry.getKey(), JsonUtils.serialize(entry.getValue()));
//        }
        // 存入redis，再次查询时，先从redis中查，查不到再从数据库查，同步到redis中
        for (SeckillGoods good : goods) {
            redisTemplate.boundHashOps(SECKILL_GOODS_KEY).put(good.getSkuId(), good);
            // 存入商品id与库存数据
            redisTemplate.opsForValue().set(good.getSkuId(), good.getStock());
        }
        return goods;
    }

    /**
     * 根据skuId，为秒杀商品集合封装秒杀库存信息
     * @param goods
     */
    private void handleStock(List<SeckillGoods> goods, List<Stock> stocks) {
        if (CollectionUtils.isEmpty(stocks)) {
            throw new LyException(LyRespStatus.GOODS_STOCK_NOT_FOUND);
        }
        // 分别得到seckill_stock和seckill_total对应skuId的map
        Map<Long, Integer> stockMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getSeckillStock));
        Map<Long, Integer> totalMap = stocks.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getSeckillTotal));
        // 根据skuId封装秒杀库存量信息
        for (SeckillGoods good : goods) {
            good.setStock(stockMap.get(good.getSkuId()));
            good.setSeckillTotal(totalMap.get(good.getSkuId()));
        }
    }

    /**
     * 删除秒杀商品
     * @param skuId
     */
    @Override
    @Transactional
    public void deleteSeckillGoods(Long skuId) {
        // 删除秒杀表
        int count = seckillMapper.deleteByPrimaryKey(skuId);
        if(count != 1){
            throw new LyException(LyRespStatus.SECKILL_SKU_SAVE_ERROR);
        }
        // 归还库存
        Stock stock = stockMapper.selectByPrimaryKey(skuId);
        if(stock == null){
            throw new LyException(LyRespStatus.GOODS_STOCK_NOT_FOUND);
        }
        Integer seckillTotal = stock.getSeckillTotal();
        stock.setSeckillTotal(0);
        stock.setSeckillStock(0);
        stock.setStock(stock.getStock() + seckillTotal); // 秒杀总量挪到总库存量上
        count = stockMapper.updateByPrimaryKeySelective(stock);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_STOCK_UPDATE_ERROR);
        }
        // 删除redis中数据
        BoundHashOperations ops = redisTemplate.boundHashOps(SECKILL_GOODS_KEY);
        if(ops.hasKey(skuId)){
            // 存在则删除秒杀商品
            ops.delete(skuId);
        }
        if(redisTemplate.hasKey(skuId)){
            // 存在则删除秒杀商品库存信息
            ops.delete(skuId);
        }
    }


}
