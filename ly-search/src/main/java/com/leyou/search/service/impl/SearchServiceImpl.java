package com.leyou.search.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate template;

    @Override
    public Goods buildGoods(Spu spu) {
        Long id = spu.getId();
        // 准备all搜索字段属性
        // 查询分类
        List<Category> categoryList = categoryClient
                .queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if(CollectionUtils.isEmpty(categoryList)){
            throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
        }
        List<String> names = categoryList.stream().map(Category::getName).collect(Collectors.toList());
        // 查询品牌
        Brand brand = brandClient.queryBrandByBid(spu.getBrandId());
        if(brand == null){
            throw new LyException(LyRespStatus.BRAND_NOT_FOUND);
        }

        String all = spu.getTitle() + StringUtils.join(names," ") + brand.getName();

        // 查询sku价格
        List<Sku> skuList = goodsClient.querySkusBySpuId(id);
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(LyRespStatus.GOODS_SKU_NOT_FOUND);
        }
        // 对skus进行处理，只存需要的一些数据
        List<Map<String, Object>> skus = new ArrayList<>();
        // 价格集合
        List<Long> prices = new ArrayList<>();
        for (Sku sku : skuList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sku.getId());
            map.put("title", sku.getTitle());
            map.put("price", sku.getPrice());
            map.put("image", StringUtils.substringBefore(sku.getImages(), ","));
            map.put("createTime", sku.getCreateTime().getTime());
            skus.add(map);
            // 处理价格
            prices.add(sku.getPrice());
        }

        // 查询规格参数
        List<SpecParam> params = specClient.queryParamsByGid(null, spu.getCid3(), true);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(LyRespStatus.SPEC_PARAM_NOT_FOUND);
        }
        // 查询商品详情
        SpuDetail spuDetail = goodsClient.queryDetail(id);
        // 获取通用规格参数
        Map<Long, String> genericSpec = JsonUtils.parseMap(
                spuDetail.getGenericSpec(), Long.class, String.class);
        // 获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.nativeRead(
                spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {});
        // 规格参数，key是规格参数的名称，值是规格参数的值
        Map<String, Object> specs = new HashMap<>();
        for (SpecParam param : params) {
            // 规格名称
            String key = param.getName();
            // 规格值
            Object value = "";
            // 判断是否是通用规格参数
            if(param.getGeneric()){
                value = genericSpec.get(param.getId());
                // 判断是否是数值类型
                if(param.getNumeric()){
                    // 处理为分段数值
                    value = chooseSegment(value.toString(), param);
                }
            }else {
                value = specialSpec.get(param.getId());
            }
            // 规格参数key和value存入specs
            specs.put(key,value);
        }

        // 创建Goods对象
        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setId(id);
        goods.setAll(all); // 搜索字段，包含标题、分类、品牌、规格等
        goods.setPrice(prices); // 所有sku的价格集合
        goods.setSkus(JsonUtils.serialize(skus)); // 所有sku的集合的json格式
        goods.setSpecs(specs); // 所有的可搜索的规格参数
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }

    /**
     * 商品搜索功能
     * @param request
     * @return
     */
    @Override
    public PageResult<Goods> search(SearchRequest request) {
        Integer page = request.getPage() - 1; // es中页码从0开始
        Integer size = request.getSize();
        // 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 添加结果过滤，过滤掉不想显示的结果
        queryBuilder.withSourceFilter(
                new FetchSourceFilter(new String[]{"id", "subTitle", "skus"},null));
        // 分页
        queryBuilder.withPageable(PageRequest.of(page,size));
        // 过滤
        QueryBuilder basicQuery = buildBasicQuery(request);
        queryBuilder.withQuery(basicQuery);
        // 聚合分类和品牌
        // 聚合分类
        String categoryAggName = "category_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        // 聚合品牌
        String brandAggName = "brand_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        // 排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if(StringUtils.isNotBlank(sortBy)){
            // 如果有排序字段，则进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy)
                    .order(desc ? SortOrder.DESC : SortOrder.ASC));
        }
        // 开始搜索
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 解析分页结果
        long total = result.getTotalElements();
        int totalPages = result.getTotalPages();
        List<Goods> goodsList = result.getContent();
        // 解析聚合结果
        Aggregations aggs = result.getAggregations();
        // 解析分类聚合结果
        List<Category> categories = parseCategory(aggs.get(categoryAggName));
        // 解析品牌聚合结果
        List<Brand> brands = parseBrand(aggs.get(brandAggName));

        // 完成规格参数聚合
        List<Map<String,Object>> specs = null;
        // 判断商品分类是否为1
        if(!CollectionUtils.isEmpty(categories) && categories.size() == 1){
            // 商品分类存在并且分类为1个，可以聚合规格参数，在原本的搜索条件基础上进行聚合
            specs = buildSpecificationAgg(categories.get(0).getId(), basicQuery);
        }
        return new SearchResult(total, totalPages, goodsList, categories, brands, specs);
    }

    /**
     * 根据商品ID，对索引库进行新增或更新
     * @param spuId
     */
    @Override
    public void createOrUpdateIndex(Long spuId) {

        // 构建goods
        Goods goods = buildGoods(goodsClient.querySpuById(spuId));
        // 存入索引库
        goodsRepository.save(goods);
    }

    /**
     * 删除索引库商品信息
     * @param spuId
     */
    @Override
    public void deleteIndex(Long spuId) {
        goodsRepository.deleteById(spuId);
    }

    private QueryBuilder buildBasicQuery(SearchRequest request) {
        // 创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        // 查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()));
        // 过滤条件
        Map<String, String> map = request.getFilter();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            // 处理key
            if(!"cid3".equals(key) && !"brandId".equals(key)){
                key = "specs." + key + ".keyword";
            }
            queryBuilder.filter(QueryBuilders.termQuery(key, entry.getValue()));
        }
        return queryBuilder;
    }

    private List<Map<String, Object>> buildSpecificationAgg(Long cid, QueryBuilder basicQuery) {
        List<Map<String, Object>> specs = new ArrayList<>();
        // 1 查询需要聚合的规格参数
        List<SpecParam> params = specClient.queryParamsByGid(null, cid, true);
        // 2 聚合
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        // 2.1 带上查询条件
        queryBuilder.withQuery(basicQuery);
        // 2.2 遍历规格参数并且聚合
        for (SpecParam param : params) {
            // 聚合
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name)
                    .field("specs." + name + ".keyword"));
        }

        // 3 获取结果
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        // 4 解析结果
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : params) {
            // 规格参数名称
            String name = param.getName();
            StringTerms terms = aggs.get(name);
            // 准备map
            Map<String, Object> map = new HashMap<>();
            map.put("key", name);
            map.put("options", terms.getBuckets()
                    .stream().map(b -> b.getKeyAsString()).collect(Collectors.toList()));
            // 添加到specs中
            specs.add(map);
        }
        return specs;
    }

    private List<Category> parseCategory(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            // 根据ids批量查询分类信息
            List<Category> categories = categoryClient.queryCategoryByIds(ids);
            return categories;
        } catch (Exception e) {
            log.error("[搜索服务]解析分类聚合失败"+e);
            return null;
        }
    }

    private List<Brand> parseBrand(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            // 根据ids批量查询品牌信息
            List<Brand> brands = brandClient.queryBrandByIds(ids);
            return brands;
        } catch (Exception e) {
            log.error("[搜索服务]解析品牌聚合失败"+e);
            return null;
        }
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }
}
