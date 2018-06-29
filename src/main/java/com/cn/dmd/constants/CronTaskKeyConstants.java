package com.cn.dmd.constants;

/**
 * 定时任务key常量
 *
 * @author morningking
 * @date 2017/7/12 14:39
 * @contact 243717042@qq.com
 */
public interface CronTaskKeyConstants {
    //执行job时锁前缀
    String JOB_LOCK_PREFIX = "JobLock";

    //作业状态列表信息前缀
    String PROCESS_STATUS_LIST_PREFIX = "JobProcessStatusList";

    //分隔符
    String SEPERATOR = "_";
}
