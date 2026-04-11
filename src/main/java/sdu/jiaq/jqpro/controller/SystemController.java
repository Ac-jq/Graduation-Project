package sdu.jiaq.jqpro.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.result.Result;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统基础接口。
 * 用于项目启动探活和统一异常链路自测。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/ping")
    public Result<Map<String, String>> ping() {
        Map<String, String> data = new HashMap<>(1);
        data.put("status", "ok");
        return Result.success("服务运行正常", data);
    }

    @GetMapping("/business-error")
    public Result<Void> businessError() {
        throw new BusinessException("这是一个用于验证全局异常处理的业务异常");
    }
}
