package item.api;

import com.leyou.common.dto.CartDTO;
import com.leyou.common.vo.PageResult;
import item.pojo.Sku;
import item.pojo.Spu;
import item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GoodsApi {
    /**
     * 分页查询spu
     * @param page
     * @param rows
     * @param saleable
     * @param key
     * @return
     */
    @GetMapping("/spu/page")
    PageResult<Spu> querySpuByPage(
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "rows",defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable",required = false) Boolean saleable,
            @RequestParam(value = "key",required = false) String key
    );

    /**
     * 根据spu的id查询detail
     * @param spuId
     * @return
     */
    @GetMapping("/spu/detail/{spuId}")
    SpuDetail queryDetail(@PathVariable("spuId") Long spuId);

    /**
     * 根据spu查询下面的所有的sku
     * @param spuId
     * @return
     */
    @GetMapping("/sku/list")
    List<Sku> querySkusBySpuId(@RequestParam("id") Long spuId);

    /**
     * 根据spu的id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    Spu querySpuById(@PathVariable("id") Long id);

    /**
     * 根据sku的id集合查询所有的sku
     * @param skuIds
     * @return
     */
    @GetMapping("/sku/list/ids")
    List<Sku> querySkusByIds(@RequestParam("ids") List<Long> skuIds);

    /**
     * 根据商品id，下单时减库存
     * @param cartDTOS
     * @return
     */
    @PostMapping("/stock/decrease")
    void decreaseStock(@RequestBody List<CartDTO> cartDTOS);

    /**
     * 根据商品id查询sku
     * @param skuId
     * @return
     */
    @GetMapping("/sku/{id}")
    Sku querySkuById(@PathVariable("id") Long skuId);
}
