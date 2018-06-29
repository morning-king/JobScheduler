package com.cn.dmd;

import com.cn.dmd.config.ApplicationStatus;
import com.cn.dmd.domain.CronJob;
import com.cn.dmd.utils.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * @author morningking
 * @date 2017/7/18 19:44
 * @contact 243717042@qq.com
 */
@Component
@Slf4j
@SuppressWarnings({"unused", "unchecked"})
public class DefaultCronJobScheduler implements CronJobScheduler {
    private final ExecutorService timeAwareJobExecutorService = Executors.newFixedThreadPool(10, new CronJobThreadFactory());
    private final ExecutorService timeNotAwareJobExecutorService = Executors.newCachedThreadPool(new CronJobThreadFactory());

    private final ConcurrentHashMap<CronJob, Boolean> timeAwareJobExecuteStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CronJob> timeNotAwareJobExecuteMap = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationStatus applicationStatus;
    @Autowired
    @Qualifier("cronJobExecutorNonAtomicWithTimeBound")
    private CronJobExecutor timeAwareJobExecutor;
    @Autowired
    @Qualifier("cronJobExecutorNonAtomicWithTimeNotBound")
    private CronJobExecutor notTimeAwareJobExecutor;
    @Autowired
    private CronTaskRegistry taskRegistry;

    @Override
    public void schedule(CronJob cronJob) {
        CronTask task = taskRegistry.getByTaskName(cronJob.getTaskName());
        CronTaskConfig taskConfig = task.getTaskConfig();

        if (!taskConfig.isTurnOn()) {
            log.info("放弃执行作业：{}，配置项配置为不执行", cronJob);
            return;
        }

        if (taskConfig.isSingletonInstanceOnly()) {
            handleTimeNotAwareJob(cronJob, taskConfig);
        } else {
            handleTimeAwareJob(cronJob, taskConfig);
        }
    }

    @Override
    public void scheduleAll(Collection<CronJob> cronJobs) {
        if (CollectionUtils.isNotEmpty(cronJobs)) {
            for (CronJob cronJob : cronJobs) {
                schedule(cronJob);
            }
        }
    }

    @PreDestroy
    private void onDestroy() {
        timeAwareJobExecutorService.shutdown();
    }

    //对于那些需要保证同一时刻只有一个作业的定时任务
    private void handleTimeAwareJob(CronJob cronJob, CronTaskConfig taskConfig) {
        if (timeAwareJobExecuteStatusMap.containsKey(cronJob)) {
            log.info("作业已在作业池中，放弃本次提交，作业信息：{}", cronJob);
            return;
        }

        timeAwareJobExecutorService.submit(() -> wrapJobExecute(cronJob, timeAwareJobExecutor,
                job -> timeAwareJobExecuteStatusMap.put(cronJob, false),
                job -> timeAwareJobExecuteStatusMap.remove(cronJob)));
    }

    //对于那些不保证同一时刻只有一个作业的定时任务
    private void handleTimeNotAwareJob(CronJob cronJob, CronTaskConfig taskConfig) {
        String taskName = taskConfig.getName();
        CronJob lastJob = timeNotAwareJobExecuteMap.putIfAbsent(taskName, cronJob);

        if (lastJob != null) {
            log.info("作业已在作业池中，放弃本次提交，本次作业信息：{}，池中作业信息：{}", cronJob, lastJob);
        } else {
            CompletableFuture.runAsync(() -> wrapJobExecute(cronJob, notTimeAwareJobExecutor,
                    null, job -> timeNotAwareJobExecuteMap.remove(job.getTaskName())), timeNotAwareJobExecutorService);
            log.info("作业成功放入作业池中，本次作业信息：{}", cronJob);
        }
    }

    private void wrapJobExecute(CronJob cronJob, CronJobExecutor executor, Function<CronJob, Object> beforeFn, Function<CronJob, Object> afterFn) {
        try {
            if (beforeFn != null) {
                beforeFn.apply(cronJob);
            }

            boolean processStatus = executor.executeJob(cronJob);

            if (processStatus) {
                LogUtil.debug(log, "任务执行成功：{}", cronJob);
            } else {
                LogUtil.warn(log, "在处理任务过程中，竞争资源失败 或者 任务已被其他应用处理：{}", cronJob);
            }
        } catch (Exception e) {
            log.error("执行任务逻辑发生错误，任务具体信息：" + cronJob, e);
        } finally {
            if (afterFn != null) {
                afterFn.apply(cronJob);
            }
        }
    }
}
