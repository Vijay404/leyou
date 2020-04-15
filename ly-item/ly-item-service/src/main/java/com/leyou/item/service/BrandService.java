package com.leyou.item.service;

import com.leyou.common.vo.PageResult;
import item.pojo.Brand;

import java.util.List;

public interface BrandService {

    PageResult<Brand> queryBrandsByPage(Integer page, Integer rows, String sortBy, Boolean desc, String key);

    void saveBrand(Brand brand, List<String> cids);

    void deleteBrand(Long id);

    Brand queryByBid(Long bid);

    List<Brand> queryBrandByCid(Long cid);

    List<Brand> queryBrandByIds(List<Long> ids);
}
