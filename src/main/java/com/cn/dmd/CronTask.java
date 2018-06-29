package com.cn.dmd;

import com.cn.dmd.domain.CronJob;

/**
 * 定时任务，包含任务的静态层面【任务配置信息、任务逻辑】
 *
 * @author morningking
 * @date 2017/7/13 17:33
 * @contact 243717042@qq.com
 */
public interface CronTask {
    /**
     * @return 任务相关的配置信息
     */
    CronTaskConfig getTaskConfig();

    /**
     * 具体业务逻辑
     */
    void invoke(CronJob cron);
}
