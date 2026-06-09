package com.qiujie.exception;

import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.enums.BusinessStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class BaseExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BaseExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO handle(ServiceException exception){
        logger.info(exception.getMessage());
        return Response.error(exception.getCode(),exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseDTO handleIllegalArgument(IllegalArgumentException exception) {
        logger.info(exception.getMessage());
        // 对于文件不存在的异常，返回 FILE_NOT_EXIST (code: 600)
        if ("文件名不能为空".equals(exception.getMessage())) {
            return Response.error(BusinessStatusEnum.FILE_NOT_EXIST);
        }
        // 其他非法参数返回 ERROR (code: 300)
        return Response.error(BusinessStatusEnum.ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseDTO handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        logger.info("数据完整性违规: " + exception.getMessage());
        // 数据库约束违反返回 ERROR (code: 300)
        return Response.error(BusinessStatusEnum.ERROR);
    }

    @ExceptionHandler(RequestRejectedException.class)
    @ResponseBody
    public ResponseDTO handleRequestRejected(RequestRejectedException exception) {
        logger.info("请求被拒绝: " + exception.getMessage());
        // 路径遍历攻击被 Spring Security 拦截，返回 ERROR (code: 300)
        return Response.error(BusinessStatusEnum.ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public ResponseDTO handleNullPointerException(NullPointerException exception) {
        logger.info("空指针异常: " + exception.getMessage());
        // 空指针异常返回 ERROR (code: 300)
        return Response.error(BusinessStatusEnum.ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseDTO handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        logger.info("类型不匹配: " + exception.getMessage());
        // 参数类型不匹配（如ID为字符串）返回 ERROR (code: 300)
        return Response.error(BusinessStatusEnum.ERROR);
    }
}
