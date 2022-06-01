package top.liujingyanghui.crypto.mybatisplus.wrapper;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoClass;
import top.liujingyanghui.crypto.mybatis.annotation.CryptoString;
import top.liujingyanghui.crypto.mybatis.util.MybatisCryptoUtil;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.baomidou.mybatisplus.core.enums.WrapperKeyword.APPLY;

/**
 * 拓展 MyBatis Plus UpdateWrapper
 * 使其在查询或更新时对声明了 @CryptoString 的实体字段进行加密查询或更新(实体对象必须加@CryptoClass注解，否则不生效)
 * 为了便于创建 UpdateWrapperX 对象，提供了 WrapperX.update(xx.class) 的快捷创建方式
 * 更推荐使用 LambdaUpdateWrapperX 方式
 * 注意：加密后不支持模糊等查询
 *
 * @author : wdh
 * @since : 2022/5/25 18:01
 */
public class UpdateWrapperX<T> extends UpdateWrapper<T> {

    private boolean isEncrypt = false;
    private final Class<T> calzz;
    /**
     * SQL 更新字段内容，例如：name='1', age=2
     */
    private final List<String> sqlSet;

    public UpdateWrapperX(Class<T> clazz) {
        this.calzz = clazz;
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(clazz, CryptoClass.class);
        if (Objects.nonNull(cryptoClass)) {
            this.isEncrypt = true;
        }
        super.initNeed();
        this.sqlSet = new ArrayList<>();
    }

    private UpdateWrapperX(T entity, List<String> sqlSet, AtomicInteger paramNameSeq,
                           Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                           SharedString lastSql, SharedString sqlComment, SharedString sqlFirst, Class<T> entityClass) {
        super.setEntity(entity);
        this.sqlSet = sqlSet;
        this.paramNameSeq = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression = mergeSegments;
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
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(sqlSet)) {
            return null;
        }
        return String.join(Constants.COMMA, sqlSet);
    }

    @Override
    public UpdateWrapperX<T> setSql(boolean condition, String sql) {
        if (condition && StringUtils.isNotBlank(sql)) {
            sqlSet.add(sql);
        }
        return this;
    }

    /**
     * 返回一个支持 lambda 函数写法的 wrapper
     */
    public LambdaUpdateWrapperX<T> lambda() {
        return new LambdaUpdateWrapperX<>(getEntity(), getEntityClass(), sqlSet, paramNameSeq, paramNameValuePairs,
                expression, paramAlias, lastSql, sqlComment, sqlFirst);
    }

    @Override
    public UpdateWrapperX<T> set(boolean condition, String column, Object val, String mapping) {
        return (UpdateWrapperX<T>) maybeDo(condition, () -> {
            String sql = formatParam(mapping, fieldEncryptHandle(column, val));
            sqlSet.add(column + Constants.EQUALS + sql);
        });
    }

    @Override
    public UpdateWrapperX<T> eq(boolean condition, String column, Object val) {
        super.eq(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public UpdateWrapperX<T> ne(boolean condition, String column, Object val) {
        super.ne(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public UpdateWrapperX<T> in(boolean condition, String column, Collection<?> coll) {
        super.in(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public UpdateWrapperX<T> in(boolean condition, String column, Object... values) {
        super.in(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public UpdateWrapperX<T> notIn(boolean condition, String column, Collection<?> coll) {
        super.notIn(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public UpdateWrapperX<T> notIn(boolean condition, String column, Object... values) {
        super.notIn(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public UpdateWrapperX<T> addNestedCondition(boolean condition, Consumer<UpdateWrapper<T>> consumer) {
        return (UpdateWrapperX<T>) maybeDo(condition, () -> {
            final UpdateWrapperX<T> instance = this.instance();
            consumer.accept(instance);
            appendSqlSegments(APPLY, instance);
        });
    }

    @Override
    protected UpdateWrapperX<T> instance() {
        return new UpdateWrapperX<>(getEntity(), null, paramNameSeq, paramNameValuePairs, new MergeSegments(),
                paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString(),this.calzz);
    }

    @Override
    public void clear() {
        super.clear();
        sqlSet.clear();
    }

    /**
     * 加密处理
     */
    private Object fieldEncryptHandle(String column, Object val) {
        if (this.isEncrypt) {
            CryptoString cryptoString = ReflectUtil.getField(this.calzz, NamingCase.toCamelCase(column)).getAnnotation(CryptoString.class);
            return MybatisCryptoUtil.fieldEncrypt(cryptoString, val);
        }
        return val;
    }


}
