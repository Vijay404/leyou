package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.service.BrandService;
import com.leyou.item.service.CategoryService;
import com.leyou.item.service.GoodsService;
import item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper detailMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;
    /**
     * 分页查询spu
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    @Override
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, String key) {
        // 分页
        PageHelper.startPage(page,rows);
        // 过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        // 搜索字段过滤
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        //上下架过滤
        if(saleable != null){
            criteria.andEqualTo("saleable",saleable); // 自动完成false为0，true为1转换
        }
        // 默认排序

        example.setOrderByClause("last_update_time DESC");
        // 查询
        List<Spu> spus = spuMapper.selectByExample(example);

        // 判断
        if(CollectionUtils.isEmpty(spus)){
            throw new LyException(LyRespStatus.GOODS_NOT_FOUND);
        }

        // 封装分类名称和品牌名称
        loadCategoryAndBrandName(spus);

        // 封装分页对象
        PageInfo<Spu> pageInfo = new PageInfo<>(spus);
        return new PageResult<Spu>(pageInfo.getTotal(),pageInfo.getPages(),spus); // TODO
    }

    /**
     * 新增商品
     * @param spu
     */
    @Transactional
    @Override
    public void saveGoods(Spu spu) {
        // 新增spu
        spu.setId(null);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        spu.setSaleable(true);
        spu.setValid(true); // (true) 1有效，0已删除

        int count = spuMapper.insert(spu);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_SAVE_ERROR);
        }
        // 新增detail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = detailMapper.insert(spuDetail);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_SAVE_ERROR);
        }

        // 新增sku和stock库存
        saveSkuAndStock(spu);

        // 发送mq消息
        sendMessage(spu.getId(), "insert");
    }

    /**
     * 根据商品ID查询spuDetail表信息
     * @param spuId
     * @return
     */
    @Override
    public SpuDetail queryDetailById(Long spuId) {
        SpuDetail spuDetail = detailMapper.selectByPrimaryKey(spuId);
        if(spuDetail == null){
            throw new LyException(LyRespStatus.GOODS_DETAIL_NOT_FOUND);
        }
        return spuDetail;
    }

    @Override
    public List<Sku> querySkusBySpuId(Long spuId) {
        // 查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> list = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.GOODS_SKU_NOT_FOUND);
        }
        // 查询库存，并封装到sku中
//        for (Sku s : list) {
//            Stock stock = stockMapper.selectByPrimaryKey(s.getId());
//            if(stock == null){
//                throw new LyException(LyRespStatus.GOODS_STOCK_NOT_FOUND);
//            }
//            s.setStock(stock.getStock());
//        }
        // 批量查询出库存
        List<Long> ids = list.stream().map(Sku::getId).collect(Collectors.toList());
        loadStockInSku(ids, list);
        return list;
    }

    /**
     * 修改商品信息
     * @param spu
     */
    @Transactional
    @Override
    public void updateGoods(Spu spu) {
        // 判断spuId是否空
        if(spu.getId() == null){
            throw new LyException(LyRespStatus.GOODS_CANNOT_BE_BULL);
        }
        // 查询出以前的sku数据，并判断是否为null
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        // 根据spuId查询所有的sku
        List<Sku> skuList = skuMapper.select(sku);
        if(!CollectionUtils.isEmpty(skuList)){
            Stock stock = new Stock();
            List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
            // 根据skuIds批量删除stock
            stockMapper.deleteByIdList(skuIds);
            // 根据spuId删除sku
            skuMapper.delete(sku);// 根据非空字段删除
        }


        // 修改spu
        spu.setLastUpdateTime(new Date());
        spu.setValid(null);
        spu.setSaleable(null);
        spu.setCreateTime(null);

        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_UPDATE_ERROR);
        }
        // 修改detail
        count = detailMapper.updateByPrimaryKeySelective(spu.getSpuDetail());
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_UPDATE_ERROR);
        }
        // 新增sku和detail
        saveSkuAndStock(spu);

        // 发送mq消息
        sendMessage(spu.getId(), "update");
    }

    /**
     * 根据商品id删除商品
     * @param id
     */
    @Transactional
    @Override
    public void deleteGoods(Long id) {
        // 判断数据库是否存在商品
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu == null){
            throw new LyException(LyRespStatus.GOODS_NOT_FOUND);
        }
        // 删除spu
        int count = spuMapper.deleteByPrimaryKey(id);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_DELETE_ERROR);
        }
        // 删除detail
        count = detailMapper.deleteByPrimaryKey(id);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_DELETE_ERROR);
        }

        // 根据skuId删除stock
        // 根据spuId查出所有的skuId
        Sku sku = new Sku();
        sku.setSpuId(id);
        List<Sku> skuList = skuMapper.select(sku);
        List<Long> skuIds = skuList.stream().map(Sku::getId).collect(Collectors.toList());
        // 批量删除stock库存
        count = stockMapper.deleteByIdList(skuIds);
        if(count == 0){
            throw new LyException(LyRespStatus.GOODS_DELETE_ERROR);
        }

        // 根据spuId删除sku
        count = skuMapper.delete(sku);
        if(count == 0){
            throw new LyException(LyRespStatus.GOODS_DELETE_ERROR);
        }

        // 发送mq消息
        sendMessage(spu.getId(), "delete");
    }

    /**
     * 修改商品上下架状态
     * @param id
     */
    @Override
    @Transactional
    public void updateSaleable(Long id ,Boolean saleable) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setSaleable(!saleable);
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if(count != 1){
            throw new LyException(LyRespStatus.GOODS_UPDATE_ERROR);
        }

        // 发送mq消息
        sendMessage(spu.getId(), "update");
    }

    /**
     * 根据spu id查询spu信息
     * @param id
     * @return
     */
    @Override
    public Spu querySpuById(Long id) {
        // 查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if(spu == null){
            throw new LyException(LyRespStatus.GOODS_NOT_FOUND);
        }

        // 查询sku
        spu.setSkus(querySkusBySpuId(id));

        // 查询spuDetail
        spu.setSpuDetail(queryDetailById(id));
        return spu;
    }

    /**
     * 根据sku的id集合查询所有的sku
     * @param ids
     * @return
     */
    @Override
    public List<Sku> querySkusByIds(List<Long> ids) {
        List<Sku> skus = skuMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(LyRespStatus.GOODS_SKU_NOT_FOUND);
        }
        loadStockInSku(ids, skus);
        return skus;
    }

    /**
     * 创建订单时，减少商品库存
     */
    @Override
    @Transactional
    public void decreaseStock(List<CartDTO> cartDTOS) {
        // 根据商品ID，修改商品库存
//        Stock stock = new Stock();
        // 注释代码存在线程安全问题，多个用户下单同一件商品时，可能会超卖。库存减为负数，直接在数据库sql判断上处理
//        for (CartDTO cartDTO : cartDTOS) {
//            stock.setSkuId(cartDTO.getSkuId());
//            // 查询出库存量，比较库存量
//            Stock dbStock = stockMapper.selectOne(stock);
//            Integer dbNum = dbStock.getStock();
//            if(dbNum >= cartDTO.getNum()){
//                stock.setStock(dbNum - cartDTO.getNum());
//            }else { // 库存不足
//                log.error("[商品服务] 商品库存不足，商品id:{}", cartDTO.getSkuId());
//                throw new LyException(LyRespStatus.GOODS_STOCK_LOW);
//            }
//        }
        // 直接新增，sql做库存数量判断即可
        for (CartDTO cartDTO : cartDTOS) {
            int count = stockMapper.updateStock(cartDTO.getNum(), cartDTO.getSkuId());
            if(count != 1){
                log.error("[商品服务] 商品库存不足，商品id:{}", cartDTO.getSkuId());
                throw new LyException(LyRespStatus.GOODS_STOCK_LOW);
            }
        }
    }

    /**
     * 根据商品sku id查询sku
     * @param skuId
     * @return
     */
    @Override
    public Sku querySkuById(Long skuId) {
        Sku sku = skuMapper.selectByPrimaryKey(skuId);
        if(sku == null){
            throw new LyException(LyRespStatus.GOODS_SKU_NOT_FOUND);
        }
        return sku;
    }

    /**
     * 查询skus的库存信息
     * @param ids
     * @param skus
     */
    private void loadStockInSku(List<Long> ids, List<Sku> skus) {
        // 批量查询出库存
        List<Stock> stockList = stockMapper.selectByIdList(ids);
        // 封装stock库存信息到每个sku中
        if (CollectionUtils.isEmpty(stockList)) {
            throw new LyException(LyRespStatus.GOODS_STOCK_NOT_FOUND);
        }
        Map<Long, Integer> stockMap = stockList.stream()
                .collect(Collectors.toMap(Stock::getSkuId, Stock::getStock));// 将skuId与stock库存值做成kv结构的map
        // 封装
        skus.forEach(s -> s.setStock(stockMap.get(s.getId())));
    }

    /**
     * spu封装分类和品牌名称
     * @param spus
     */
    private void loadCategoryAndBrandName(List<Spu> spus) {
        for (Spu spu : spus) {
            // 封装分类名称，不使用字符串拼接，因为会造成内存占用多
            List<String> names = categoryService.queryCategoryListByCids(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names,"/"));

            // 处理品牌名称
            spu.setBname(brandService.queryByBid(spu.getBrandId()).getName());
        }
    }

    /**
     * 新增sku和库存方法
     * @param spu
     */
    private void saveSkuAndStock(Spu spu){
        // 库存集合
        List<Stock> stocks = new ArrayList<>();
        // 新增sku
        List<Sku> skus = spu.getSkus();
        for (Sku sku : skus) {
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            sku.setSpuId(spu.getId());

            int count = skuMapper.insert(sku);
            if(count != 1){
                throw new LyException(LyRespStatus.GOODS_SAVE_ERROR);
            }
            Stock stock = new Stock();
            stock.setStock(sku.getStock());
            stock.setSkuId(sku.getId());
            stocks.add(stock);
        }

        // 批量新增库存
        int count = stockMapper.insertList(stocks);
        if(count == 0){
            throw new LyException(LyRespStatus.GOODS_SAVE_ERROR);
        }

        // 发送mq消息
        sendMessage(spu.getId(), "insert");
    }

    private void sendMessage(Long id, String type){
        // 发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (Exception e) {
            log.error("[商品服务mq]：发送消息失败！" + e + "[商品id]：" + id);
        }
    }
}
