package com.leyou.item.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.service.BrandService;
import item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper brandMapper;

    @Override
    public PageResult<Brand> queryBrandsByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        // 分页
        PageHelper.startPage(page,rows);

        /*
            where `name` like '%x%' or letter == 'x'
            order by id desc
         */
        // 过滤
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
            // 添加过滤条件
            example.createCriteria()
                    .orLike("name","%"+key+"%")
                    .orEqualTo("letter",key.toUpperCase());
        }
        // 排序
        if(StringUtils.isNotBlank(sortBy)){
            String orderByClause = sortBy+(desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }
        // 查询
        List<Brand> list = brandMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.BRAND_NOT_FOUND);
        }
        PageInfo<Brand> pageInfo = new PageInfo<>(list);
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getList());
    }

    @Transactional
    @Override
    public void saveBrand(Brand brand, List<String> cids) {
        // 使用传递的brand对象的主键是否为空，判断是修改还是新增操作
        if(brand.getId() != null){ // 修改品牌操作
            int count = brandMapper.updateByPrimaryKey(brand);
            if(count != 1){
                throw new LyException(LyRespStatus.BRAND_UPDATE_ERROR);
            }
            // 先删除brand对应的分类，再重新插入
            brandMapper.deleteCategoryBrand(brand.getId());
            // 根据cids重新插入数据
            for (String cid : cids) {
                count = brandMapper.insertCategoryBrand(cid, brand.getId());
                if(count != 1){
                    throw new LyException(LyRespStatus.BRAND_SAVE_ERROR);
                }
            }
        }else { // 新增品牌操作
            brand.setId(null);
            int count = brandMapper.insert(brand);
            if(count != 1){
                throw new LyException(LyRespStatus.BRAND_SAVE_ERROR);
            }
            // 新增中间表数据
            for (String cid : cids) {
                count = brandMapper.insertCategoryBrand(cid, brand.getId());
                if(count != 1){
                    throw new LyException(LyRespStatus.BRAND_SAVE_ERROR);
                }
            }
        }
    }

    @Override
    public void deleteBrand(Long id) {
        // 删除品牌
        int count = brandMapper.deleteByPrimaryKey(id);
        if(count != 1){
            throw new LyException(LyRespStatus.BRAND_DELETE_ERROR);
        }

        // 删除分类
        count = brandMapper.deleteCategoryBrand(id);
        if(count != 1){
            throw new LyException(LyRespStatus.BRAND_DELETE_ERROR);
        }
    }

    /**
     * 根据品牌ID查询品牌信息
     * @param bid
     * @return
     */
    @Override
    public Brand queryByBid(Long bid) {
        Brand brand = brandMapper.selectByPrimaryKey(bid);
        if(brand == null){
            throw new LyException(LyRespStatus.BRAND_NOT_FOUND);
        }
        return brand;
    }

    @Override
    public List<Brand> queryBrandByCid(Long cid) {
        List<Brand> list = brandMapper.queryBrandByCid(cid);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.BRAND_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据ids批量查询品牌
     * @param ids
     * @return
     */
    @Override
    public List<Brand> queryBrandByIds(List<Long> ids) {
        List<Brand> list = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.BRAND_NOT_FOUND);
        }
        return list;
    }
}
