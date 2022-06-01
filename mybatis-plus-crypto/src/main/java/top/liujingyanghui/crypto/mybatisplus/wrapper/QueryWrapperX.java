package top.liujingyanghui.crypto.mybatisplus.wrapper;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
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
 * 拓展 MyBatis Plus QueryWrapper
 * 使其在查询时对声明了 @CryptoString 的实体字段进行加密查询(实体对象必须加@CryptoClass注解，否则不生效)
 * 为了便于创建 QueryWrapperX 对象，提供了 WrapperX.query(xx.class) 的快捷创建方式
 * 更推荐使用 LambdaQueryWrapperX方式
 * 注意：加密后不支持模糊等查询
 *
 * @author : wdh
 * @since : 2022/5/25 17:30
 */
public class QueryWrapperX<T> extends QueryWrapper<T> {

    private boolean isEncrypt = false;
    private final Class<T> calzz;
    private SharedString sqlSelect = new SharedString();

    public QueryWrapperX(Class<T> clazz) {
        this.calzz = clazz;
        CryptoClass cryptoClass = AnnotationUtil.getAnnotation(clazz, CryptoClass.class);
        if (Objects.nonNull(cryptoClass)) {
            this.isEncrypt = true;
        }
        super.initNeed();
    }

    QueryWrapperX(T entity, Class<T> entityClass, AtomicInteger paramNameSeq,
                  Map<String, Object> paramNameValuePairs, MergeSegments mergeSegments, SharedString paramAlias,
                  SharedString lastSql, SharedString sqlComment, SharedString sqlFirst) {
        super.setEntity(entity);
        super.setEntityClass(entityClass);
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
    
    /**
     * 返回一个支持 lambda 函数写法的 wrapper
     */
    public LambdaQueryWrapperX<T> lambda() {
        return new LambdaQueryWrapperX<>(getEntity(), this.calzz, sqlSelect, paramNameSeq, paramNameValuePairs,
                expression, paramAlias, lastSql, sqlComment, sqlFirst);
    }
    

    @Override
    public QueryWrapperX<T> eq(boolean condition, String column, Object val) {
        super.eq(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public QueryWrapperX<T> ne(boolean condition, String column, Object val) {
        super.ne(condition, column, fieldEncryptHandle(column, val));
        return this;
    }

    @Override
    public QueryWrapperX<T> in(boolean condition, String column, Collection<?> coll) {
        super.in(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public QueryWrapperX<T> in(boolean condition, String column, Object... values) {
        super.in(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public QueryWrapperX<T> notIn(boolean condition, String column, Collection<?> coll) {
        super.notIn(condition, column, (Collection<?>) fieldEncryptHandle(column, coll));
        return this;
    }

    @Override
    public QueryWrapperX<T> notIn(boolean condition, String column, Object... values) {
        super.notIn(condition, column, (Object[]) fieldEncryptHandle(column, values));
        return this;
    }

    @Override
    public QueryWrapperX<T> addNestedCondition(boolean condition, Consumer<QueryWrapper<T>> consumer) {
        return (QueryWrapperX<T>) maybeDo(condition, () -> {
            final QueryWrapperX<T> instance = this.instance();
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
     * <p>
     * 故 sqlSelect 不向下传递
     * </p>
     */
    @Override
    protected QueryWrapperX<T> instance() {
        return new QueryWrapperX<>(getEntity(), this.calzz, paramNameSeq, paramNameValuePairs, new MergeSegments(),
                paramAlias, SharedString.emptyString(), SharedString.emptyString(), SharedString.emptyString());
    }

    @Override
    public void clear() {
        super.clear();
        sqlSelect.toNull();
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
