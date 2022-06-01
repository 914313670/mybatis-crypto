package top.liujingyanghui.crypto.mybatisplus.wrapper;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoClass;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.baomidou.mybatisplus.core.enums.WrapperKeyword.APPLY;

/**
 * 拓展 MyBatis Plus LambdaUpdateWrapper
 * 使其在查询或更新时对声明了 @CryptoString 的实体字段进行加密查询或更新(实体对象必须加@CryptoClass注解，否则不生效)
 * 为了便于创建 LambdaUpdateWrapperX 对象，提供了 WrapperX.lambdaUpdate(xx.class) 的快捷创建方式
 * 注意：加密后不支持模糊等查询
 *
 * @author : wdh
 * @since : 2022/5/25 17:25
 */
public class LambdaUpdateWrapperX<T> extends LambdaUpdateWrapper<T> {

    private boolean isEncrypt = false;
    private final Class<T> calzz;
    /**
     * SQL 更新字段内容，例如：name='1', age=2
     */
    private final List<String> sqlSet;

    public LambdaUpdateWrapperX(Class<T> clazz) {
        this.calzz = clazz;
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(clazz, CryptoClass.class);
        if (Objects.nonNull(cryptoClass)) {
            this.isEncrypt = true;
        }
        super.initNeed();
        this.sqlSet = new ArrayList<>();
    }

    LambdaUpdateWrapperX(T entity, Class<T> entityClass, List<String> sqlSet, AtomicInteger paramNameSeq,
                         Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                         SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.sqlSet = sqlSet;
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
        this.calzz = entityClass;
        CryptoString cryptoString = AnnotationUtil.getAnnotation(entityClass, CryptoString.class);
        if (Objects.nonNull(cryptoString)) {
            this.isEncrypt = true;
        }
    }

    @Override
    public LambdaUpdateWrapperX<T> setSql(boolean condition, String sql) {
        if (condition && StringUtils.isNotBlank(sql)) {
            sqlSet.add(sql);
        }
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> set(boolean condition, SFunction<T, ?> column, Object val, String mapping) {
        return (LambdaUpdateWrapperX<T>) maybeDo(condition, () -> {
            String sql = formatParam(mapping, fieldEncryptHandle(column, val));
            sqlSet.add(columnToString(column) + Constants.EQUALS + sql);
        });
    }

    @Override
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(sqlSet)) {
            return null;
        }
        return String.join(Constants.COMMA, sqlSet);
    }

    @Override
    public LambdaUpdateWrapperX<T> eq(boolean condition, SFunction<T, ?> column, Object val) {
        super.eq(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> ne(boolean condition, SFunction<T, ?> column, Object val) {
        super.ne(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        super.in(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        super.in(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        super.notIn(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        super.notIn(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public LambdaUpdateWrapperX<T> addNestedCondition(boolean condition, Consumer<LambdaUpdateWrapper<T>> consumer) {
        return (LambdaUpdateWrapperX<T>) maybeDo(condition, () -> {
            final LambdaUpdateWrapperX<T> instance = this.instance();
            consumer.accept(instance);
            appendSqlSegments(APPLY, instance);
        });
    }

    @Override
    protected LambdaUpdateWrapperX<T> instance() {
        return new LambdaUpdateWrapperX<>(getEntity(), this.calzz, null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSet.clear();
    }

    /**
     * 加密处理
     */
    private Object fieldEncryptHandle(SFunction<T, ?> column, Object val) {
        if (this.isEncrypt) {
            CryptoString cryptoString = ReflectUtil.getField(this.calzz, columnsToString(column)).getAnnotation(CryptoString.class);
            return MybatisCryptoUtil.fieldEncrypt(cryptoString, val);
        }
        return val;
    }

}
