package com.cn.dmd.utils;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * bean转换工具类
 *
 * @author morningking
 * @since 2018/4/24 09:47
 */
@Slf4j
public class BeanUtil {
    /**
     * 对象之间的属性拷贝
     *
     * @param source     源对象
     * @param clazz      目前类信息
     * @param <T>        目标类型
     * @param ignoreArgs 忽略字段列表
     * @return 目标对象
     */
    public static <T> T convert(Object source, Class<T> clazz, String... ignoreArgs) {
        if (source == null) {
            return null;
        } else {
            Preconditions.checkNotNull(clazz, "clazz参数不能为空");

            try {
                T target = clazz.newInstance();
                BeanUtils.copyProperties(source, target, ignoreArgs);

                return target;
            } catch (InstantiationException | IllegalAccessException e) {
                log.error("实例化类：" + clazz.getCanonicalName() + "失败", e);

                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 对象列表之间的属性拷贝
     *
     * @param sourceList 源对象列表
     * @param clazz      目前类信息
     * @param <T>        目标类型
     * @param ignoreArgs 忽略字段列表
     * @return 目标对象列表
     */
    public static <T> List<T> convertToList(Collection<?> sourceList, Class<T> clazz, String... ignoreArgs) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        } else {
            return sourceList.stream().map(source -> convert(source, clazz, ignoreArgs)).collect(Collectors.toList());
        }
    }

    /**
     * 对象列表之间的属性拷贝
     *
     * @param sourceList 源对象列表
     * @param clazz      目前类信息
     * @param ignoreArgs 忽略字段列表
     * @param <T>        目标类型
     * @return 目标对象集合
     */
    public static <T> Set<T> convertToSet(Collection<?> sourceList, Class<T> clazz, String... ignoreArgs) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptySet();
        } else {
            return sourceList.stream().map(source -> convert(source, clazz, ignoreArgs)).collect(Collectors.toSet());
        }
    }
}
