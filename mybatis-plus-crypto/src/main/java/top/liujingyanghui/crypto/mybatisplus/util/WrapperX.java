package top.liujingyanghui.crypto.mybatisplus.util;

import top.liujingyanghui.crypto.mybatisplus.wrapper.LambdaQueryWrapperX;
import top.liujingyanghui.crypto.mybatisplus.wrapper.LambdaUpdateWrapperX;
import top.liujingyanghui.crypto.mybatisplus.wrapper.QueryWrapperX;
import top.liujingyanghui.crypto.mybatisplus.wrapper.UpdateWrapperX;

/**
 * @author : wdh
 * @since : 2022/5/25 14:08
 */
public class WrapperX<T> {

    /**
     * 获取 LambdaQueryWrapperX
     *
     * @param <T> 实体类泛型
     * @return LambdaQueryWrapperX
     */
    public static <T> LambdaQueryWrapperX<T> lambdaQuery(Class<T> entityClass) {
        return new LambdaQueryWrapperX<>(entityClass);
    }

    /**
     * 获取 QueryWrapperX
     *
     * @param <T> 实体类泛型
     * @return QueryWrapperX
     */
    public static <T> QueryWrapperX<T> query(Class<T> entityClass) {
        return new QueryWrapperX<>(entityClass);
    }

    /**
     * 获取 LambdaUpdateWrapperX
     *
     * @param <T> 实体类泛型
     * @return LambdaUpdateWrapperX
     */
    public static <T> LambdaUpdateWrapperX<T> lambdaUpdate(Class<T> entityClass) {
        return new LambdaUpdateWrapperX<>(entityClass);
    }

    /**
     * 获取 UpdateWrapperX
     *
     * @param <T> 实体类泛型
     * @return UpdateWrapperX
     */
    public static <T> UpdateWrapperX<T> update(Class<T> entityClass) {
        return new UpdateWrapperX<>(entityClass);
    }

}
