package com.market.config;

import com.market.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)   // 返回 400 而不是 500，更合适
    public Result<String> handleRuntimeException(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : "操作失败";
        return Result.error(400, message);
    }
}