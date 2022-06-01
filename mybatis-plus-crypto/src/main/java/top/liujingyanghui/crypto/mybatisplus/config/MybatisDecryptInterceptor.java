package top.liujingyanghui.crypto.mybatisplus.config;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.model.CryptKeyModel;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.*;

/**
 * mybatis 结果解密拦截器
 *
 * @author : wdh
 * @since : 2022/4/16 12:36
 */
@Component
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class MybatisDecryptInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //取出查询的结果
        Object resultObject = invocation.proceed();
        if (Objects.isNull(resultObject)) {
            return null;
        }

        if (resultObject instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> coll = (Collection<Object>) resultObject;
            if (CollectionUtils.isEmpty(coll)) {
                return coll;
            }
            Object obj = CollUtil.getFirst(coll);
            if (MybatisCryptoUtil.hasCryptoClass(obj)) { // 泛型为实体
                MybatisCryptoUtil.paramCrypto(resultObject, null, CryptoMode.DECRYPT);
            } else if (obj instanceof Map) { // 泛型为Map
                MappedStatement mappedStatement = getMappedStatement((DefaultResultSetHandler) invocation.getTarget());
                Set<CryptKeyModel> models = MybatisCryptoUtil.mappedStatement2MapperCryptoModel(mappedStatement, CryptoMode.DECRYPT);
                if (CollUtil.isNotEmpty(models)) {
                    MybatisCryptoUtil.paramCrypto(resultObject, models, CryptoMode.DECRYPT);
                    return resultObject;
                }
                return resultObject;
            } else if (obj instanceof String) { // 泛型为String
                MappedStatement mappedStatement = getMappedStatement((DefaultResultSetHandler) invocation.getTarget());
                String namespace = MybatisCryptoUtil.getNamespace(mappedStatement);
                Method method = MybatisCryptoUtil.getMethodByNamespace(namespace);
                CryptoString cryptoString = AnnotationUtil.getAnnotation(method, CryptoString.class);
                if (Objects.isNull(cryptoString)) {
                    return resultObject;
                }
                if (cryptoString.mode().equals(CryptoMode.ENCRYPT)) {
                    return resultObject;
                }
                Collection<Object> result = CollUtil.create(String.class);
                ICryptoRule cryptoRule = cryptoString.rule().newInstance();
                for (Object item : coll) {
                    result.add(cryptoRule.decrypt((String) item));
                }
                return result;
            }
        } else {
            if (MybatisCryptoUtil.hasCryptoClass(resultObject)) {
                MybatisCryptoUtil.entityCrypto(resultObject, CryptoMode.DECRYPT);
            }
        }
        return resultObject;
    }

    /**
     * 获取MappedStatement对象
     */
    private MappedStatement getMappedStatement(DefaultResultSetHandler resultSetHandler) throws NoSuchFieldException, IllegalAccessException {
        Class<DefaultResultSetHandler> handlerClass = DefaultResultSetHandler.class;
        Field mappedStatementFiled = handlerClass.getDeclaredField("mappedStatement");
        mappedStatementFiled.setAccessible(true);
        return (MappedStatement) mappedStatementFiled.get(resultSetHandler);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
