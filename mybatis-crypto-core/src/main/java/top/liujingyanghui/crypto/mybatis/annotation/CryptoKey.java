package top.liujingyanghui.crypto.mybatis.annotation;

import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.rule.AbstractCryptoRule;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;

import java.lang.annotation.*;

/**
 * Map对象敏感信息
 *
 * @author : wdh
 * @since : 2022/5/23 15:15
 */
@Inherited
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoKey {

    /**
     * map中的key名
     */
    String key() default "";

    /**
     * 加密规则
     */
    Class<? extends ICryptoRule> rule() default AbstractCryptoRule.class;

    /**
     * 加解密方式
     */
    CryptoMode mode() default CryptoMode.ALL;

}
