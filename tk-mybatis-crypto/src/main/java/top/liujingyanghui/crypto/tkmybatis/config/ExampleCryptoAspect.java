package top.liujingyanghui.crypto.tkmybatis.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;
import top.liujingyanghui.crypto.tkmybatis.util.TkMybatisCryptoUtil;

/**
 * tkmapper example加密
 *
 * @author ：wdh
 * @since ：Created in 2022/4/24 23:04
 */
@Aspect
@Component
public class ExampleCryptoAspect {

    @Pointcut("execution(public * tk.mybatis.mapper.common.example.*.*(..))")
    public void cut() {
    }

    @Before("cut()")
    public void deBefore(JoinPoint jp) {
        for (Object arg : jp.getArgs()) {
            if (arg instanceof Example) {
                Example example = ((Example) arg);
                try {
                    TkMybatisCryptoUtil.exampleEncrypt(example);
                } catch (Exception e) {
                }
            }
        }
    }

    @AfterReturning("cut()")
    public void doAfterReturning(JoinPoint jp) {
        for (Object arg : jp.getArgs()) {
            if (arg instanceof Example) {
                Example example = ((Example) arg);
                try {
                    TkMybatisCryptoUtil.exampleDecrypt(example);
                } catch (Exception e) {
                }
            }
        }
    }

}
