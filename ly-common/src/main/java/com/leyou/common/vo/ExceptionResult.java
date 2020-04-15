package com.leyou.common.vo;

import com.leyou.common.enums.LyRespStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异常结果对象
 */
@Data
public class ExceptionResult {
    private int status;
    private String message;
    private Long timestamp;

    public ExceptionResult(LyRespStatus lyRespStatus) {
        this.status = lyRespStatus.getCode();
        this.message = lyRespStatus.getMsg();
        this.timestamp = System.currentTimeMillis();
    }
}
