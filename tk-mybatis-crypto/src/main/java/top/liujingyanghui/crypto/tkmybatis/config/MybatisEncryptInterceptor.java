package top.liujingyanghui.crypto.tkmybatis.config;

import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import tk.mybatis.mapper.entity.Example;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.enums.CryptoMode;
import top.liujingyanghui.crypto.mybatis.model.CryptKeyModel;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;
import top.liujingyanghui.crypto.tkmybatis.util.TkMybatisCryptoUtil;

import java.lang.reflect.Method;
import java.util.*;

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

        // 处理使用Example查询情况
        if (paramObject instanceof Example) {
            return invocation.proceed();
        }

        // 如果查询参数为单个String
        if (paramObject instanceof String) {
            MybatisCryptoUtil.singleStringEncryptHandle(namespace, args);
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

        // 去除Example对象，防止克隆报错
        Map<String, Example> exampleMap = MapUtil.newHashMap();
        Iterator<Map.Entry<String, Object>> iterator = parameterObjectMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> next = iterator.next();
            if (next.getValue() instanceof Example) {
                exampleMap.put(next.getKey(), (Example) next.getValue());
                iterator.remove();
            }
        }

        Object cloneMap = MybatisCryptoUtil.mapClone(parameterObjectMap, namespace);
        if (cloneMap instanceof Map) {
            Map<String, Object> cloneMapTemp = (Map<String, Object>) cloneMap;

            if (MapUtil.isNotEmpty(exampleMap)) {
                Set<Map.Entry<String, Example>> entries = exampleMap.entrySet();
                for (Map.Entry<String, Example> entry : entries) {
                    cloneMapTemp.put(entry.getKey(), entry.getValue());
                    parameterObjectMap.put(entry.getKey(), entry.getValue());
                }
            }
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

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }


}
