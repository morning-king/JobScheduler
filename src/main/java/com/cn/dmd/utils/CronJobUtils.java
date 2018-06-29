package com.cn.dmd.utils;

import com.cn.dmd.domain.CronJob;
import com.cn.dmd.CronTaskConfig;
import org.springframework.util.Assert;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author morningking
 * @date 2017/9/1 15:09
 * @contact 243717042@qq.com
 */
public class CronJobUtils {
    /**
     * @param taskConfig 任务对应的配置信息
     * @return 【对于频率为分钟的定时任务】计算出当前被调度到的任务对应的定时作业
     * @throws IllegalArgumentException 目前仅支持小时内，若干分钟一次的语义
     */
    public static CronJob getCronJobInMinuteByNow(CronTaskConfig taskConfig) {
        final String taskName = taskConfig.getName();
        final long minuteStartRule = taskConfig.getTaskStartRule();
        final long intervalInMinute = taskConfig.getTaskControlTimeDuration();

        Assert.isTrue(intervalInMinute < 54 && intervalInMinute > 0, "无法根据此表达式计算出作业");
        LocalDateTime now = LocalDateTime.now();
        now = now.truncatedTo(ChronoUnit.MINUTES);

        long minuteInHour = now.getMinute();
        long fromMinute = minuteStartRule;

        while (fromMinute <= minuteInHour) {
            fromMinute += intervalInMinute;
        }

        now = now.minus(intervalInMinute - fromMinute + minuteInHour, ChronoUnit.MINUTES);

        return new CronJob(taskName, getMilliSeconds(now.minus(intervalInMinute, ChronoUnit.MINUTES)), getMilliSeconds(now));
    }

    /**
     * @param taskConfig 任务配置
     * @return 【对于频率为小时的定时任务】计算出当前被调度到的任务对应的定时作业
     */
    public static CronJob getCronJobInHourByNow(CronTaskConfig taskConfig) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        return new CronJob(taskConfig.getName(), getMilliSeconds(now.minus(taskConfig.getTaskControlTimeDuration(), ChronoUnit.HOURS)),
                getMilliSeconds(now));
    }

    private static long getMilliSeconds(LocalDateTime time) {
        return Timestamp.valueOf(time).getTime();
    }
}
