package com.leyou.item.service;

import item.pojo.Brand;
import item.pojo.Category;

import java.util.List;

public interface CategoryService {
    List<Category> queryCategoryListByPid(Long pid);

    List<Category> queryCategoryByBid(Long bid);

    List<Category> queryCategoryListByCids(List<Long> cids);

    void saveCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(Long id);

    List<Category> queryAllCategoryLevelByCid3(Long id);
}
