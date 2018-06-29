package com.cn.dmd;

import com.cn.dmd.domain.CronJob;

import java.util.Collection;

/**
 * 定时作业调度器
 *
 * @author morningking
 * @date 2017/7/12 14:27
 * @contact 243717042@qq.com
 */
public interface CronJobScheduler {
    /**
     * 提交一个定时任务
     *
     * @param cronJob 定时任务
     */
    void schedule(CronJob cronJob);

    /**
     * 批量提交定时任务
     *
     * @param cronJobs 定时任务集合
     */
    void scheduleAll(Collection<CronJob> cronJobs);
}
