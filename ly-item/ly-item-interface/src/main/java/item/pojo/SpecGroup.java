package item.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;
import javax.persistence.*;
import java.util.List;

@Data
@Table(name = "tb_spec_group")
public class
SpecGroup {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Long id;

    private Long cid;

    private String name;

    // 忽略字段
//    @JsonIgnore // 返回页面时，忽略此字段
    @Transient
    private List<SpecParam> params; // 不属于这张表的字段
}