package top.liujingyanghui.crypto.mybatisplus.wrapper;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoClass;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.baomidou.mybatisplus.core.enums.WrapperKeyword.APPLY;

/**
 * 拓展 MyBatis Plus LambdaQueryWrapper
 * 使其在查询时对声明了@CryptoString的实体字段进行加密查询(实体对象必须加@CryptoClass注解，否则不生效)
 * 为了便于创建LambdaQueryWrapperX对象，提供了 WrapperX.lambdaQuery(xx.class) 的快捷创建方式
 * 注意：加密后不支持模糊等查询
 *
 * @author : wdh
 * @since : 2022/5/25 17:25
 */
public class LambdaQueryWrapperX<T> extends LambdaQueryWrapper<T> {

    private boolean isEncrypt = false;
    private final Class<T> calzz;
    private SharedString sqlSelect = new SharedString();

    public LambdaQueryWrapperX(Class<T> clazz) {
        this.calzz = clazz;
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(clazz, CryptoClass.class);
        if (Objects.nonNull(cryptoClass)) {
            this.isEncrypt = true;
        }
        super.initNeed();
    }

    LambdaQueryWrapperX(T entity, Class<T> entityClass, SharedString sqlSelect, AtomicInteger paramNameSeq,
                        Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                        SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
        this.sqlSelect = sqlSelect;
        this.paramAlias = paramAlias;
        this.lastSql = lastSql;
        this.sqlComment = sqlComment;
        this.sqlFirst = sqlFirst;
        this.calzz = entityClass;
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(entityClass, CryptoClass.class);
        if (Objects.nonNull(cryptoClass)) {
            this.isEncrypt = true;
        }
    }

    @Override
    public LambdaQueryWrapperX<T> eq(boolean condition, SFunction<T, ?> column, Object val) {
        super.eq(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> ne(boolean condition, SFunction<T, ?> column, Object val) {
        super.ne(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> in(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        super.in(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> in(boolean condition, SFunction<T, ?> column, Object... values) {
        super.in(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> notIn(boolean condition, SFunction<T, ?> column, Collection<?> coll) {
        super.notIn(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> notIn(boolean condition, SFunction<T, ?> column, Object... values) {
        super.notIn(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public LambdaQueryWrapperX<T> addNestedCondition(boolean condition, Consumer<LambdaQueryWrapper<T>> consumer) {
        return (LambdaQueryWrapperX<T>) maybeDo(condition, () -> {
            final LambdaQueryWrapperX<T> instance = this.instance();
            consumer.accept(instance);
            appendSqlSegments(APPLY, instance);
        });
    }

    @Override
    public String getSqlSelect() {
        return sqlSelect.getStringValue();
    }

    /**
     * 用于生成嵌套 sql
     * <p>故 sqlSelect 不向下传递</p>
     */
    @Override
    protected LambdaQueryWrapperX<T> instance() {
        return new LambdaQueryWrapperX<>(getEntity(), this.calzz, null, paramNameSeq, paramNameValuePairs,
                new MergeSegments(), paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
    }

    /**
     * 字段加密处理
     */
    private Object fieldEncryptHandle(SFunction<T, ?> column, Object val) {
        if (this.isEncrypt) {
            CryptoString cryptoString = ReflectUtil.getField(this.calzz, columnsToString(column)).getAnnotation(CryptoString.class);
            return MybatisCryptoUtil.fieldEncrypt(cryptoString, val);
        }
        return val;
    }

}
