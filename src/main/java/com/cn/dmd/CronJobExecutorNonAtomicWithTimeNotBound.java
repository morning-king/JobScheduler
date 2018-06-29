package com.cn.dmd;

import com.cn.dmd.domain.CronJob;
import com.cn.dmd.utils.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 使用此执行器来执行这些定时作业：
 * 1. 对时间信息敏感【需要依赖当前时间来划分作业范围】，一般是那些查询全部条件的定时任务；
 * 2. 不需要原子提交【使用redis，无法事务提交】；
 *
 * @author morningking
 * @date 2017/8/22 16:21
 * @contact 243717042@qq.com
 */
@Component
@Slf4j
public class CronJobExecutorNonAtomicWithTimeNotBound implements CronJobExecutor {
    @Autowired
    private CronJobRedisActions cronJobRedisActions;
    @Autowired
    private CronTaskRegistry cronTaskRegistry;
    //锁续命间隔
    @Value("${cron.general.lock-inspiration-interval}")
    private int INSPIRE_LOCK_INTERVAL;

    @Override
    public boolean executeJob(CronJob cronJob) {
        final CronJob convertedJob = new CronJob(cronJob.getTaskName(), 0, 0);
        boolean processStatus = false;

        log.info("成功记录作业开始：{}", cronJob);

        if (log.isInfoEnabled()) {
            log.info("尝试获取作业执行权，作业信息：{}", convertedJob);
        }

        boolean isLockHold = cronJobRedisActions.tryLock(convertedJob);

        if (!isLockHold) {
            if (log.isInfoEnabled()) {
                log.info("获取作业执行权失败，放弃处理，作业信息：{}", convertedJob);
            }
        } else {
            ScheduledThreadPoolExecutor timer = null;

            try {
                log.info("获取到作业的执行权，开始执行作业：{}", convertedJob);

                //为持有的锁定时续上超时时间
                timer = new ScheduledThreadPoolExecutor(1);
                timer.scheduleWithFixedDelay(() -> {
                    try {
                        cronJobRedisActions.inspireLock(convertedJob);
                        if (log.isInfoEnabled()) {
                            log.info("为作业关联锁续上超时时间成功：{}", cronJob);
                        }
                    } catch (Exception e) {
                        log.warn("为作业关联锁续上超时时间失败：" + convertedJob, e);
                    }
                }, INSPIRE_LOCK_INTERVAL, INSPIRE_LOCK_INTERVAL, TimeUnit.SECONDS);

                //执行业务逻辑
                CronTask cronTask = cronTaskRegistry.getByTaskName(convertedJob.getTaskName());
                if (cronTask.getTaskConfig().isAlwaysSucceed()) {
                    LogUtil.info(log, "该作业被标记为总是成功：{}", cronJob);
                } else {
                    cronTask.invoke(cronJob);
                }

                //释放锁时失败并不影响整个作业的执行状态
                //锁是临时性的数据，会自动超时
                try {
                    cronJobRedisActions.deleteLock(convertedJob);
                } catch (Exception e) {
                    log.info("释放作业互斥锁时发生失败，不影响作业执行状态，作业信息：" + convertedJob, e);
                }

                log.info("成功执行作业：{}", convertedJob);
                processStatus = true;
            } catch (Exception e) {
                cronJobRedisActions.deleteLock(convertedJob);

                throw e;
            } finally {
                if (timer != null) {
                    timer.shutdown();
                    log.info("成功关闭锁续时的定时线程，相关作业为：{}", convertedJob);
                }
            }
        }

        return processStatus;
    }
}
