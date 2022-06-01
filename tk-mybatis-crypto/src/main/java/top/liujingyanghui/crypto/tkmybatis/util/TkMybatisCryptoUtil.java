package top.liujingyanghui.crypto.tkmybatis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.core.annotation.AnnotationUtils;
import tk.mybatis.mapper.entity.Example;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoClass;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * tk mybatis 加解密工具类
 *
 * @author : wdh
 * @since : 2022/5/30 15:25
 */
public class TkMybatisCryptoUtil {

    /**
     * Example 对象加密
     *
     * @param example example对象
     */
    public static void exampleEncrypt(Example example) throws InstantiationException, IllegalAccessException {
        Class<?> entityClass = example.getEntityClass();
        CryptoClass cryptoClass = AnnotationUtils.findAnnotation(entityClass, CryptoClass.class);
        if (Objects.isNull(cryptoClass)) {
            return;
        }
        for (Example.Criteria e : example.getOredCriteria()) {
            for (Example.Criterion c : e.getAllCriteria()) {
                Object value = c.getValue();
                String condition = c.getCondition();
                if (value != null && !StrUtil.isEmpty(condition)) {
                    String[] strings = condition.split(" ");
                    if (strings.length > 0) {
                        Field field = ReflectUtil.getField(entityClass, NamingCase.toCamelCase(strings[0]));
                        CryptoString cryptoString = field.getAnnotation(CryptoString.class);
                        if (Objects.nonNull(cryptoString)) {
                            Class<? extends ICryptoRule> rule = cryptoString.rule();
                            ICryptoRule cryptoRule = rule.newInstance();
                            if (value instanceof String) {
                                ReflectUtil.setFieldValue(c, "value", cryptoRule.encrypt((String) value));
                            } else if (value instanceof List) {
                                List valueList = (List) value;
                                if (CollUtil.isNotEmpty(valueList) && valueList.get(0) instanceof String) {
                                    for (int i = 0; i < valueList.size(); i++) {
                                        valueList.set(i, cryptoRule.encrypt((String) valueList.get(i)));
                                    }
                                }
                                ReflectUtil.setFieldValue(c, "value", valueList);
                            }
                        }
                    }
                }
            }
        }
    }
}
