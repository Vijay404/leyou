package com.leyou.common.adviceHandler;

import com.leyou.common.exception.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice // 自动拦截Controller注解类
public class CommonExceptionHandler {

    @ExceptionHandler(LyException.class) //拦截指定的异常
    public ResponseEntity<ExceptionResult> handleException(LyException e) {
        return ResponseEntity.status(e.getLyRespStatus().getCode())
                .body(new ExceptionResult(e.getLyRespStatus()));
    }
}
