package com.macro.mall.common.service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性Service接口
 * @author zhenwu
 * @date 2024/10/19 15:27
 */
public interface IdempotenceService {

    String KEY_PREFIX = "idempotence:";

    String requestToken(String key, long timeout, TimeUnit timeUnit);

    boolean validToken(String key, String token);
}