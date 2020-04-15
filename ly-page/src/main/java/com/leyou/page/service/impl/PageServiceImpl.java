package com.leyou.page.service.impl;

import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import com.leyou.page.service.PageService;
import item.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PageServiceImpl implements PageService {

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 加载商品详情页面渲染需要的数据
     * @param spuId
     * @return
     */
    @Override
    public Map<String, Object> loadAttrs(Long spuId) {
        Map<String, Object> model = new HashMap<>();
        // 查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        // 获取skus
        List<Sku> skus = spu.getSkus();
        // 查询spuDetail
        SpuDetail spuDetail = goodsClient.queryDetail(spuId);
        // 查询brand
        Brand brand = brandClient.queryBrandByBid(spu.getBrandId());
        // 查询商品分类
        List<Category> categories = categoryClient.queryCategoryByIds(
                Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        // 查询规格组groups
        List<SpecGroup> groups = specClient.queryGroupListByCid(spu.getCid3());
        // 获取规格参数

        model.put("title",spu.getTitle());
        model.put("subTitle",spu.getSubTitle());
        model.put("skus",skus);
        model.put("spuDetail",spuDetail);
        model.put("groups",groups);
        model.put("brand",brand);
        model.put("categories",categories);
        return model;
    }

    /**
     * 根据商品id生成html静态页面
     * @param spuId
     */
    @Override
    public void createHtml(Long spuId){
        // 上下文对象
        Context context = new Context();
        context.setVariables(loadAttrs(spuId));
        // 输出流
        File file = new File("D:/uploads", spuId+".html");
        if(file.exists()){
            // 存在则删除
            file.delete();
        }

        try {
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            // 生成html
            templateEngine.process("item", context, writer);
        } catch (Exception e) {
            log.error("[静态页服务] 生成静态页面失败！" + e);
        }
    }

    /**
     * 根据商品id，删除对应的静态页
     * @param spuId
     */
    @Override
    public void deleteHtml(Long spuId) {
        // 输出流
        File file = new File("D:/uploads", spuId+".html");
        if(file.exists()){
            // 存在则删除
            file.delete();
        }
    }
}
