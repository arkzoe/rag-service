package com.rag.backend.common.exception;

import com.rag.backend.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 这里可以添加日志记录
        // log.error("系统异常: ", e);
        return Result.error("系统繁忙，请稍后再试：" + e.getMessage());
    }

}