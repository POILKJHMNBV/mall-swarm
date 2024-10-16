package com.macro.mall.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * @author zhenwu
 * @date 2023/10/3 14:48
 */
public final class RegexUtils {

    private RegexUtils() {
    }

    /**
     * 是否是无效手机格式
     *
     * @param username 要校验的用户名
     * @return true:符合，false：不符合
     */
    public static boolean invalidUsername(String username) {
        return mismatch(username, RegexPatterns.USERNAME_REGEX);
    }

    /**
     * 是否是无效手机格式
     *
     * @param password 要校验的密码
     * @return true:符合，false：不符合
     */
    public static boolean invalidPassword(String password) {
        return mismatch(password, RegexPatterns.PASSWORD_REGEX);
    }

    /**
     * 是否是无效手机格式
     *
     * @param phone 要校验的手机号
     * @return true:符合，false：不符合
     */
    public static boolean invalidPhone(String phone) {
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }

    /**
     * 是否是无效邮箱格式
     *
     * @param email 要校验的邮箱
     * @return true:符合，false：不符合
     */
    public static boolean invalidEmail(String email) {
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
     * 是否是无效验证码格式
     *
     * @param code 要校验的验证码
     * @return true:符合，false：不符合
     */
    public static boolean invalidAuthCode(String code) {
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    // 校验是否不符合正则格式
    private static boolean mismatch(String str, String regex) {
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
