package cn.edu.guet.mycms.exception;

import cn.edu.guet.mycms.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PermissionException.class)
    public Result<Object> handlePermissionException(PermissionException e) {
        // 返回 403 状态码（Forbidden）或业务约定的错误码
        return Result.fail(403, e.getMessage());
    }
    @ExceptionHandler(IllegalUserException.class)
    public Result<Object> handlePermissionException(IllegalUserException e) {
        // 返回 401 状态码（Unauthorized）或业务约定的错误码
        return Result.fail(401, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage());
    }
}

