package com.cn.dmd;

import com.cn.dmd.domain.CronJob;

/**
 * 作业执行器，包装具体任务逻辑，同时提供一套完整的作业执行流程
 *
 * @author morningking
 * @date 2017/7/18 19:45
 * @contact 243717042@qq.com
 */
public interface CronJobExecutor {
    /**
     * 执行定时作业
     *
     * @param cronJob 定时作业
     * @return 是否成功处理定时作业
     */
    boolean executeJob(CronJob cronJob);
}
