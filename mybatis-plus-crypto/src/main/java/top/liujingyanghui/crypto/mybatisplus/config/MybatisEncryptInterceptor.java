package top.liujingyanghui.crypto.mybatisplus.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.core.annotation.AnnotationUtils;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.model.CryptKeyModel;
import top.liujingyanghui.crypto.mybatis.rule.ICryptoRule;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * 查询参数加密拦截器
 *
 * @author ：wdh
 * @since ：Created in 2022/5/28 13:57
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
public class MybatisEncryptInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatements = (MappedStatement) args[0];
        String namespace = MybatisCryptoUtil.getNamespace(mappedStatements);
        Object paramObject = args[1];
        if (Objects.isNull(paramObject)) {
            return invocation.proceed();
        }

        // 如果查询参数为单个String
        if (paramObject instanceof String) {
            singleEncryptHandle(namespace, args);
            return invocation.proceed();
        }

        // 如果参数为单个实体对象
        if (!(paramObject instanceof Map)) {
            MybatisCryptoUtil.singleEntityEncryptHandle(namespace, args);
            return invocation.proceed();
        }

        Method method = MybatisCryptoUtil.getMethodByNamespace(namespace);
        if (Objects.isNull(method)) {
            return invocation.proceed();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parameterObjectMap = (Map<String, Object>) paramObject;

        CryptoString cryptoString = MybatisCryptoUtil.getCryptoStringByMethod(method, CryptoMode.ENCRYPT);
        if (Objects.nonNull(cryptoString)) { // 入参为单个 List<String>、String[]的情况
            if (cryptoString.mode().equals(CryptoMode.ALL) || cryptoString.mode().equals(CryptoMode.ENCRYPT)) {
                MybatisCryptoUtil.stringsEncryptHandle(cryptoString, parameterObjectMap);
                return invocation.proceed();
            }
        }

        // HashMap 实现了Serializable，可以进行深拷贝
        Object cloneMap = MybatisCryptoUtil.mapClone(parameterObjectMap, namespace);
        if (cloneMap instanceof Map) {
            Map<String, Object> cloneMapTemp = (Map<String, Object>) cloneMap;
            Object page = cloneMapTemp.get("page");
            if (Objects.nonNull(page) && page instanceof Page) {
                cloneMapTemp.put("page", parameterObjectMap.get("page"));
                args[1] = cloneMapTemp;
            } else {
                args[1] = cloneMap;
            }
        } else {
            args[1] = cloneMap;
        }

        Set<CryptKeyModel> models = MybatisCryptoUtil.mappedStatement2MapperCryptoModel(mappedStatements, CryptoMode.ENCRYPT);
        MybatisCryptoUtil.paramCrypto(cloneMap, models, CryptoMode.ENCRYPT);

        return invocation.proceed();
    }

    /**
     * 单个String入参加密处理
     *
     * @param namespace 方法命名空间
     * @param args      参数
     */
    private void singleEncryptHandle(String namespace, Object[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Method method = MybatisCryptoUtil.getMethodByNamespace(namespace);
        CryptoString cryptoString = AnnotationUtils.findAnnotation(method, CryptoString.class);
        if (Objects.isNull(cryptoString)) {
            return;
        }
        if (cryptoString.mode().equals(CryptoMode.DECRYPT)) {
            return;
        }

        ICryptoRule cryptoRule = cryptoString.rule().newInstance();
        args[1] = cryptoRule.encrypt((String) args[1]);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }


}
