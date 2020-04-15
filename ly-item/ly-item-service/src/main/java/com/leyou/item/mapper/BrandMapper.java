package com.leyou.item.mapper;

import com.leyou.common.mapper.BaseMapper;
import item.pojo.Brand;
import org.apache.ibatis.annotations.*;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand, Long> {

    /**
     * 新增品牌分类中间表
     * @param cid
     * @param bid
     * @return
     */
    @Insert("INSERT INTO tb_category_brand(category_id, brand_id) VALUES (#{cid},#{bid})")
    int insertCategoryBrand(@Param("cid") String cid,@Param("bid") Long bid);

    /**
     * 修改品牌分类中间表
     * @param cid
     * @param bid
     * @return
     */
    @Update("update tb_category_brand set category_id = #{cid} where brand_id = #{bid}")
    int updateCategoryBrand(@Param("cid") String cid,@Param("bid") Long bid);

    /**
     * 删除品牌分类
     * @param cid
     * @param bid
     * @return
     */
    @Update("delete from tb_category_brand where brand_id = #{bid}")
    int deleteCategoryBrand(Long bid);

    @Select("SELECT * FROM tb_brand WHERE id IN (SELECT brand_id FROM tb_category_brand WHERE category_id = #{cid})")
    List<Brand> queryBrandByCid(@Param("cid") Long cid);
}
