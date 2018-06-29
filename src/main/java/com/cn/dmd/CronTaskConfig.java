package com.cn.dmd;

import com.cn.dmd.constants.CronScheduleTimeUnit;
import lombok.Data;

/**
 * @author morningking
 * @date 2017/7/14 15:51
 * @contact 243717042@qq.com
 */
@Data
public class CronTaskConfig {
    //任务名称
    private final String name;
    //是否需要启动时回溯
    private final boolean isNeedScanBacktrace;
    //是否需要错误检测
    private final boolean isNeedErrorDetect;
    //是否只需要保持集群单实例，忽略时间段信息
    private final boolean isSingletonInstanceOnly;
    //任务时间粒度
    private final CronScheduleTimeUnit cronScheduleTimeUnit;
    /**
     * 每次任务关联的时间跨度，需根据{@link CronScheduleTimeUnit unit}来综合计算
     */
    private final long taskControlTimeDuration;
    //获取应用启动时需要回溯的最大时间范围【小时】
    private final long loadScanBacktraceTimeDuration;
    //任务开始规则（根据任务时间粒度区分：【为分钟时：在小时内第几分钟开始；为小时时：在24小时内第几小时开始（最大23）】）
    private final int taskStartRule;
    //扫描执行失败作业的时间间隔（秒）
    private final long scanFailedJobsIntervalInSeconds;
    //是否开启
    private final boolean isTurnOn;
    //是否永远成功
    private final boolean isAlwaysSucceed;

    public CronTaskConfig(String name, boolean isNeedScanBacktrace, boolean isNeedErrorDetect, boolean isSingletonInstanceOnly,
                          CronScheduleTimeUnit cronScheduleTimeUnit, long taskControlTimeDuration, long loadScanBacktraceTimeDuration,
                          int taskStartRule, long scanFailedJobsIntervalInSeconds, boolean isTurnOn, boolean isAlwaysSucceed) {
        this.name = name;
        this.isNeedScanBacktrace = isNeedScanBacktrace;
        this.isNeedErrorDetect = isNeedErrorDetect;
        this.isSingletonInstanceOnly = isSingletonInstanceOnly;
        this.cronScheduleTimeUnit = cronScheduleTimeUnit;
        this.taskControlTimeDuration = taskControlTimeDuration;
        this.loadScanBacktraceTimeDuration = loadScanBacktraceTimeDuration;
        this.taskStartRule = taskStartRule;
        this.scanFailedJobsIntervalInSeconds = scanFailedJobsIntervalInSeconds;
        this.isTurnOn = isTurnOn;
        this.isAlwaysSucceed = isAlwaysSucceed;
    }

    long getDurationInMilliSeconds() {
        long duration = -1L;

        switch (cronScheduleTimeUnit) {
            case MINUTE:
                duration = taskControlTimeDuration * 60 * 1000;
                break;
            case HOUR:
                duration = taskControlTimeDuration * 3600 * 1000;
                break;
            default:
                break;
        }

        return duration;
    }
}
