package com.macro.mall.common.util;

/**
 * @author zhenwu
 * @date 2023/10/3 14:42
 */
public interface RegexPatterns {

    /**
     * 用户名正则
     */
    String USERNAME_REGEX = "^[\\w]{4,16}$";

    /**
     * 手机号正则
     */
    String PHONE_REGEX = "^1([38][0-9]|4[579]|5[0-3,5-9]|6[6]|7[0135678]|9[89])\\d{8}$";

    /**
     * 邮箱正则
     */
    String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

    /**
     * 密码正则。4~32位的字母、数字、下划线
     */
    String PASSWORD_REGEX = "^\\w{4,32}$";

    /**
     * 验证码正则, 6位数字
     */
    String VERIFY_CODE_REGEX = "^[0-9]{6}$";
}
