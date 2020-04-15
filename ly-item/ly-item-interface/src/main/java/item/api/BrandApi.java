package item.api;

import item.pojo.Brand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface BrandApi {
    /**
     * 根据id查询品牌
     * @param bid
     * @return
     */
    @GetMapping("/brand/{bid}")
    Brand queryBrandByBid(@PathVariable("bid")Long bid);

    /**
     * 根据ids批量查询品牌
     * @param ids
     * @return
     */
    @GetMapping("/brand/list")
    List<Brand> queryBrandByIds(@RequestParam("ids")List<Long> ids);
}
