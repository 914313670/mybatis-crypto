package top.liujingyanghui.crypto.mybatis.annotation;

import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.rule.AbstractCryptoRule;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;

import java.lang.annotation.*;

/**
 * 敏感信息String类型（包括泛型为String的集合和数组）字段
 *
 * @author : wdh
 * @since : 2022/5/23 11:07
 */
@Inherited
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoString {

    /**
     * 加解密规则
     */
    Class<? extends ICryptoRule> rule() default AbstractCryptoRule.class;

    /**
     * 加解密方式
     */
    CryptoMode mode() default CryptoMode.ALL;

}
