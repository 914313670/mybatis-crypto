package top.liujingyanghui.crypto.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 敏感信息类
 * 声明后才会继续扫描注解类下的其它敏感信息注解
 *
 * @author : wdh
 * @since : 2022/5/23 13:59
 */
@Inherited
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoClass {
}
