package com.cn.dmd;

import com.cn.dmd.domain.CronJob;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 作业扫描器
 * 1. 启动时，计算缺失的作业；
 * 2. 定时扫描集群中失败的作业
 *
 * @author morningking
 * @date 2017/7/12 14:26
 * @contact 243717042@qq.com
 */
@Component
@Slf4j
public class CronJobScanner {
    @Autowired
    private CronJobRedisActions cronJobRedisActions;
    @Autowired
    private CronJobScheduler cronJobScheduler;
    @Autowired
    private CronTaskRegistry cronTaskRegistry;
    private volatile ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    /**
     * 监听启动事件
     */
    @EventListener
    private void onApplicationContextStart(ApplicationReadyEvent event) {
        log.info("开始扫描缺失执行的作业");
        scanForMissingJobs();

        log.info("开始启动定时扫描失败的作业");
        scanForUnCompletedJobs();
    }

    @PreDestroy
    private void onPreDestroy() {
        if (scheduledThreadPoolExecutor != null) {
            scheduledThreadPoolExecutor.shutdown();
        }
    }

    /**
     * 启动时，扫描对比那些缺失执行的任务
     */
    private void scanForMissingJobs() {
        for (String taskName : cronTaskRegistry.getAllTaskNames()) {
            if (log.isDebugEnabled()) {
                log.debug("开始扫描任务[{}]缺失的作业", taskName);
            }

            CronTask cronTask = cronTaskRegistry.getByTaskName(taskName);
            CronTaskConfig taskConfig = cronTask.getTaskConfig();
            if (taskConfig.isNeedScanBacktrace()) {
                scanForMissingJobs(taskName);
            }

            if (log.isDebugEnabled()) {
                log.debug("任务[{}]缺失的作业扫描结束", taskName);
            }
        }
    }

    private void scanForMissingJobs(String taskName) {
        CronTask cronTask = cronTaskRegistry.getByTaskName(taskName);
        CronTaskConfig taskConfig = cronTask.getTaskConfig();

        Set<Long> watchedTimes = Collections.emptySet();
        switch (taskConfig.getCronScheduleTimeUnit()) {
            case MINUTE:
                watchedTimes = getAllTimesInMinute(taskName, taskConfig);
                break;
            case HOUR:
                watchedTimes = getAllTimesInHour(taskName, taskConfig);
                break;
        }

        if (CollectionUtils.isNotEmpty(watchedTimes)) {
            Set<CronJob> candicateCronJobs = getCandicateCronJobs(watchedTimes, taskConfig);
            if (CollectionUtils.isNotEmpty(candicateCronJobs)) {
                if (log.isInfoEnabled()) {
                    log.info("发现任务[{}]缺失作业：{}", taskName, candicateCronJobs);
                }

                cronJobScheduler.scheduleAll(candicateCronJobs);
            }
        }
    }

    private Set<Long> getAllTimesInMinute(String taskName, CronTaskConfig taskConfig) {
        Assert.isTrue(taskConfig.getTaskStartRule() >= 0 && taskConfig.getTaskStartRule() <= 60, "当使用分钟为单元时，开始时间应该在0-60之间");
        Assert.isTrue(taskConfig.getTaskControlTimeDuration() > 0, "定时任务间隔时间应该大于0");
        Assert.isTrue(taskConfig.getLoadScanBacktraceTimeDuration() > 0, "回溯小时数应该大于0");

        Set<Long> watchedTimes = Sets.newTreeSet();

        LocalDateTime now = LocalDateTime.now();
        now = now.truncatedTo(ChronoUnit.MINUTES);

        //这里判定业务定时任务肯定是在一个小时内固定分钟数开启的
        int minuteInHour = now.getMinute();
        int fromMinute = taskConfig.getTaskStartRule();
        while (fromMinute <= minuteInHour) {
            fromMinute += taskConfig.getTaskControlTimeDuration();
        }
        now = now.minus(taskConfig.getTaskControlTimeDuration() - fromMinute + minuteInHour, ChronoUnit.MINUTES);

        LocalDateTime fromTime = now.minus(taskConfig.getLoadScanBacktraceTimeDuration(), ChronoUnit.HOURS);

        while (!now.isEqual(fromTime)) {
            watchedTimes.add(Timestamp.valueOf(fromTime).getTime() - taskConfig.getDurationInMilliSeconds());
            fromTime = fromTime.plus(taskConfig.getTaskControlTimeDuration(), ChronoUnit.MINUTES);
        }
        watchedTimes.add(Timestamp.valueOf(now).getTime() - taskConfig.getDurationInMilliSeconds());

        Set<Long> recordStartTimeSet = cronJobRedisActions.getJobsOfTask(taskName);
        watchedTimes.removeAll(recordStartTimeSet);

        return watchedTimes;
    }

    private Set<Long> getAllTimesInHour(String taskName, CronTaskConfig taskConfig) {
        Assert.isTrue(taskConfig.getTaskStartRule() >= 0 && taskConfig.getTaskStartRule() <= 23, "当使用小时为单元时，开始时间应该在0-23之间");
        Assert.isTrue(taskConfig.getLoadScanBacktraceTimeDuration() > 0, "回溯小时数应该大于0");

        Set<Long> watchedTimes = Sets.newTreeSet();

        LocalDateTime now = LocalDateTime.now();
        now = now.truncatedTo(ChronoUnit.HOURS);

        int nowHour = now.getHour();
        int fromHour = taskConfig.getTaskStartRule();
        if (nowHour > fromHour) {
            now = now.withHour(fromHour);
        } else if (nowHour < fromHour) {
            now = now.withHour(fromHour).minusDays(1);
        }

        LocalDateTime fromTime = now.minus(taskConfig.getLoadScanBacktraceTimeDuration(), ChronoUnit.HOURS);

        while (!now.isEqual(fromTime)) {
            watchedTimes.add(Timestamp.valueOf(fromTime).getTime() - taskConfig.getDurationInMilliSeconds());
            fromTime = fromTime.plus(taskConfig.getTaskControlTimeDuration(), ChronoUnit.HOURS);
        }
        watchedTimes.add(Timestamp.valueOf(fromTime).getTime() - taskConfig.getDurationInMilliSeconds());

        Set<Long> recordStartTimeSet = cronJobRedisActions.getJobsOfTask(taskName);
        watchedTimes.removeAll(recordStartTimeSet);

        return watchedTimes;
    }

    /**
     * 定时扫描未完成的任务列表
     */
    private void scanForUnCompletedJobs() {
        Set<String> taskNames = cronTaskRegistry.getAllTaskNames();
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(taskNames.size());

        for (String taskName : cronTaskRegistry.getAllTaskNames()) {
            CronTask cronTask = cronTaskRegistry.getByTaskName(taskName);
            CronTaskConfig taskConfig = cronTask.getTaskConfig();

            if (taskConfig.isNeedErrorDetect()) {
                scanForUnCompletedJobs(taskName);
            }
        }
    }

    private void scanForUnCompletedJobs(String taskName) {
        CronTask cronTask = cronTaskRegistry.getByTaskName(taskName);
        CronTaskConfig taskConfig = cronTask.getTaskConfig();

        scheduledThreadPoolExecutor.scheduleWithFixedDelay(() -> {
            if (log.isInfoEnabled()) {
                log.info("开始扫描任务[{}]相关的作业", taskName);
            }

            Set<Long> jobStartTimeSet = cronJobRedisActions.getJobsOfTask(taskName);
            if (CollectionUtils.isNotEmpty(jobStartTimeSet)) {
                Set<CronJob> candicateCronJobs = getCandicateCronJobs(jobStartTimeSet, taskConfig);
                if (CollectionUtils.isNotEmpty(candicateCronJobs)) {
                    if (log.isInfoEnabled()) {
                        log.info("扫描到失败的作业并提交至任务池：{}", candicateCronJobs);
                    }

                    cronJobScheduler.scheduleAll(candicateCronJobs);
                }
            }
        }, taskConfig.getScanFailedJobsIntervalInSeconds(), taskConfig.getScanFailedJobsIntervalInSeconds(), TimeUnit.SECONDS);
    }

    //获取那些符合条件，可以被安排执行的作业【条件：作业状态为未完成且未与锁关联】
    private Set<CronJob> getCandicateCronJobs(Collection<Long> cronJobStartTimes, CronTaskConfig taskConfig) {
        List<CronJob> cronJobs = cronJobStartTimes.stream().map(
                startTime -> new CronJob(taskConfig.getName(), startTime, startTime + taskConfig.getDurationInMilliSeconds()))
                .collect(Collectors.toList());
        Map<CronJob, Boolean> cronJobStatusMap = cronJobRedisActions.getStatusOfJobs(cronJobs);

        if (MapUtils.isNotEmpty(cronJobStatusMap)) {
            //第一步：过滤出那些未完成的作业列表
            List<CronJob> unCompletedJobs = cronJobStatusMap.entrySet().stream()
                    .filter(entry -> !Boolean.TRUE.equals(entry.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(unCompletedJobs)) {
                Map<CronJob, Boolean> cronJobLockHoldStatusMap = cronJobRedisActions.getStatusOfJobLockHoldInfo(unCompletedJobs);
                if (MapUtils.isNotEmpty(cronJobLockHoldStatusMap)) {
                    //第二步：过滤出那些未与锁关联的作业列表
                    return cronJobLockHoldStatusMap.entrySet().stream().filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                            .map(Map.Entry::getKey).collect(Collectors.toSet());
                }
            }
        }

        return Collections.emptySet();
    }
}
