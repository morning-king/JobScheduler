package com.cn.dmd;

import java.util.Set;

/**
 * @author morningking
 * @date 2017/7/13 17:29
 * @contact 243717042@qq.com
 */
public interface CronTaskRegistry {
    /**
     * @param taskName 任务名称
     * @return 根据任务名称获取对应的任务
     */
    CronTask getByTaskName(String taskName);

    /**
     * 注册任务名和具体任务的映射
     *
     * @param taskName 任务名
     * @param cronTask 任务
     */
    void register(String taskName, CronTask cronTask);

    /**
     * @return 获取在该注册器中注册过的所有的任务名称
     */
    Set<String> getAllTaskNames();
}
