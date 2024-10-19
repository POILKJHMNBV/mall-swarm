package com.macro.mall.common.service.impl;

import cn.hutool.core.util.IdUtil;
import com.macro.mall.common.service.IdempotenceService;
import com.macro.mall.common.service.RedisService;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhenwu
 * @date 2024/10/19 15:39
 */
@Service
public class IdempotenceServiceImpl implements IdempotenceService {

    private static final DefaultRedisScript<Long> VALID_TOKEN_SCRIPT;

    static {
        VALID_TOKEN_SCRIPT = new DefaultRedisScript<>();
        VALID_TOKEN_SCRIPT.setLocation(new ClassPathResource("validToken.lua"));
        VALID_TOKEN_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private RedisService redisService;

    @Override
    public String requestToken(String key, long timeout, TimeUnit timeUnit) {
        String token = IdUtil.simpleUUID();
        redisService.set(key, token, timeUnit.toSeconds(timeout));
        return token;
    }

    @Override
    public boolean validToken(String key, String token) {
        List<String> keys = Collections.singletonList(key);
        // 执行lua脚本，保证原子性，返回值为0L表示成功，否则失败
        return redisService.execute(VALID_TOKEN_SCRIPT, keys, token).equals(0L);
    }
}
