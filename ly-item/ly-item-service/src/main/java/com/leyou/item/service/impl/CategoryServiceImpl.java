package com.leyou.item.service.impl;

import com.leyou.common.enums.LyRespStatus;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.service.CategoryService;
import item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;


    @Override
    public List<Category> queryCategoryListByPid(Long pid) {
        //查询条件，通用mapper对象中的非空字段拼接到查询条件中
        Category t = new Category();
        t.setParentId(pid);
        List<Category> list = categoryMapper.select(t);
        // 判断查询结果
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
        }
        return list;
    }

    @Override
    public List<Category> queryCategoryByBid(Long bid) {
        List<Category> list = categoryMapper.queryCategoryByBid(bid);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
        }
        return list;
    }

    /**
     * 根据分类id查询分类信息集合
     * @param cids
     * @return
     */
    @Override
    public List<Category> queryCategoryListByCids(List<Long> cids) {
        List<Category> list = categoryMapper.selectByIdList(cids);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
        }
        return list;
    }

    /**
     * 保存分类信息
     * @param category
     */
    @Transactional
    @Override
    public void saveCategory(Category category) {
        // 保存当前分类信息
        int count = categoryMapper.insert(category);
        // 根据parentId查出父节点信息，判断其父节点的is_parent字段是否为1，不是则更新
        if(category.getParentId() != null){// 有父节点
            // 根据父节点ID查询父节点信息
            Category parent = categoryMapper.selectByPrimaryKey(category.getParentId());
            if(parent == null){
                throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
            }
            if(!parent.getIsParent()){
                // 修改父节点标记为true，并更新
                parent.setIsParent(true);
                categoryMapper.updateByPrimaryKey(parent);
            }
        }
        if(count != 1){
            throw new LyException(LyRespStatus.CATEGORY_SAVE_ERROR);
        }
    }

    /**
     * 更新分类信息
     * @param category
     */
    @Transactional
    @Override
    public void updateCategory(Category category) {
        int count = categoryMapper.updateByPrimaryKeySelective(category);
        if(count != 1){
            throw new LyException(LyRespStatus.CATEGORY_UPDATE_ERROR);
        }
    }

    /**
     * 根据分类id删除分类信息
     * @param id
     */
    @Transactional
    @Override
    public void deleteCategory(Long id) {
        // 删除前查询当前节点是否为父节点
        Category c1 = categoryMapper.selectByPrimaryKey(id);
        if(c1.getIsParent()){// 是父节点，不允许直接删除
            throw new LyException(LyRespStatus.CATEGORY_DELETE_ERROR);
        }
        int count = 0;
        // 根据parent_id字段查询当前节点有无兄弟节点，没有则设置其父节点的is_parent字段为0
        Category category = new Category();
        category.setParentId(c1.getParentId());
        List<Category> categoryList = categoryMapper.select(category);
        if(categoryList.size() == 1){ // 刚刚删除未提交的子分类，即没有兄弟节点
            // 没有兄弟节点，设置当前节点的父节点的is_parent字段为0
            category.setIsParent(false);
            // 切换到父节点
            category.setId(c1.getParentId());
            category.setParentId(null);
            count = categoryMapper.updateByPrimaryKeySelective(category);
            if(count != 1){
                throw new LyException(LyRespStatus.CATEGORY_UPDATE_ERROR);
            }
        }

        // 删除当前节点
        count = categoryMapper.deleteByPrimaryKey(id);
        if(count != 1){
            throw new LyException(LyRespStatus.CATEGORY_DELETE_ERROR);
        }

    }

    /**
     * 根据cid3查询其所有层级分类
     * @param id
     * @return
     */
    @Override
    public List<Category> queryAllCategoryLevelByCid3(Long id) {
        List<Category> categoryList = new ArrayList<>();
        Category category = this.categoryMapper.selectByPrimaryKey(id);
        if(category == null){
            throw new LyException(LyRespStatus.CATEGORY_NOT_FOUND);
        }
        categoryList.add(category);
        while (category.getParentId() != 0){
            category = this.categoryMapper.selectByPrimaryKey(category.getParentId());
            categoryList.add(category);
        }
        return categoryList;
    }
}
