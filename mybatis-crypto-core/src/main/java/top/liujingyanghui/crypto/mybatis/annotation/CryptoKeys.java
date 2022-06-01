package top.liujingyanghui.crypto.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 多个Map敏感信息
 *
 * @author : wdh
 * @since : 2022/5/23 15:15
 */
@Inherited
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoKeys {

    /**
     * 敏感信息
     */
    CryptoKey[] value() default {};

}
