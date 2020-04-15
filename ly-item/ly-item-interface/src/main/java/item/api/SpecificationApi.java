package item.api;

import item.pojo.SpecGroup;
import item.pojo.SpecParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface SpecificationApi {
    /**
     * 根据组id查询规格参数
     * @param gid
     * @return
     */
    @GetMapping("/spec/params")
    List<SpecParam> queryParamsByGid(
            @RequestParam(value = "gid",required = false) Long gid,
            @RequestParam(value = "cid",required = false) Long cid,
            @RequestParam(value = "searching",required = false) Boolean searching
    );

    /**
     * 根据分类id查询规格组信息
     * @param cid
     * @return
     */
    @GetMapping("/spec/group")
    List<SpecGroup> queryGroupListByCid(@RequestParam("cid")Long cid);

    /**
     * 根据规格参数id集合查询规格参数
     * @param ids
     * @return
     */
    @GetMapping("/spec/param/ids")
    List<SpecParam> queryParamByIds(@RequestParam("ids") List<Long> ids);
}
