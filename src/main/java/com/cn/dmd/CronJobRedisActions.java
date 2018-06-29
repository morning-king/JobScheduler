package com.cn.dmd;

import com.cn.dmd.config.ApplicationStatus;
import com.cn.dmd.constants.CronTaskKeyConstants;
import com.cn.dmd.domain.CronJob;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.springframework.data.redis.connection.RedisStringCommands.SetOption.SET_IF_ABSENT;

/**
 * 定时作业相关redis操作
 *
 * @author morningking
 * @date 2017/7/12 17:03
 * @contact 243717042@qq.com
 */
@Component
@Slf4j
public class CronJobRedisActions {
    private static final String JOB_STATUS_UN_COMPLETED = "false";
    private static final String JOB_STATUS_COMPLETED = "true";
    private static final String FORMAT_PATTERN = "yyyy-MM-dd HH:mm:SS";
    private static final DateParser dateParser = FastDateFormat.getInstance(FORMAT_PATTERN);
    //锁持有的时间
    @Value("${cron.general.lock-time}")
    private int cronJobLockExpirationTime;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ApplicationStatus applicationStatus;

    /**
     * @param taskName 任务名
     * @return 获取指定任务名相关的作业开始时间列表
     */
    public Set<Long> getJobsOfTask(String taskName) {
        Set<String> keys = redisTemplate.<String, String>opsForHash().keys(getProcessListKeyName(taskName));

        Set<Long> recordTaskTimeInfos = Collections.emptySet();
        if (CollectionUtils.isNotEmpty(keys)) {
            recordTaskTimeInfos = keys.stream().map(this::parseDate).filter(time -> time > 0).collect(Collectors.toSet());
        }

        return recordTaskTimeInfos;
    }

    /**
     * @param cronJob 作业
     * @return 获取作业执行状态【null：不存在，1：未完成，0：已完成】
     */
    public Boolean getStatusOfJob(CronJob cronJob) {
        String status = redisTemplate.<String, String>opsForHash().get(
                getProcessListKeyName(cronJob.getTaskName()), formatDate(cronJob.getStartTime()));

        return isJobCompleted(status);
    }

    /**
     * @param cronJobs 作业列表
     * @return 批量获取作业列表对应的时刻信息
     */
    public Map<CronJob, Boolean> getStatusOfJobs(List<CronJob> cronJobs) {
        if (CollectionUtils.isEmpty(cronJobs)) {
            return Collections.emptyMap();
        } else {
            String taskName = cronJobs.iterator().next().getTaskName();
            List<Boolean> jobStatusList = redisTemplate.<String, String>opsForHash().multiGet(getProcessListKeyName(taskName),
                    cronJobs.stream().map(cronJob -> formatDate(cronJob.getStartTime())).collect(Collectors.toList()))
                    .stream().map(this::isJobCompleted).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(jobStatusList) || jobStatusList.size() != cronJobs.size()) {
                throw new IllegalStateException("返回作业状态信息数量有误：期望" + cronJobs.size() + "，返回：" + CollectionUtils.size(jobStatusList));
            } else {
                Map<CronJob, Boolean> statusMap = Maps.newHashMapWithExpectedSize(cronJobs.size());
                Iterator<Boolean> statusIterator = jobStatusList.iterator();

                cronJobs.forEach(cronJob -> statusMap.put(cronJob, statusIterator.next()));

                return statusMap;
            }
        }
    }

    /**
     * 如果作业信息在redis不存在，创建之
     *
     * @param cronJob 作业信息
     */
    public Boolean createJobIfNotPresent(CronJob cronJob) {
        return redisTemplate.execute((RedisCallback<Boolean>) conn -> conn.hSetNX(getProcessListKeyName(cronJob.getTaskName()).getBytes(),
                (formatDate(cronJob.getStartTime())).getBytes(), JOB_STATUS_UN_COMPLETED.getBytes()));
    }

    /**
     * 设置作业已完成
     *
     * @param cronJob 作业信息
     */
    public void markJobCompleted(CronJob cronJob) {
        redisTemplate.opsForHash().put(getProcessListKeyName(cronJob.getTaskName()), formatDate(cronJob.getStartTime()), JOB_STATUS_COMPLETED);
    }

    /**
     * 尝试获取对任务在指定时刻
     *
     * @param cronJob 非阻塞式的获取任务对应的锁
     * @return 是否获取成功
     */
    public boolean tryLock(CronJob cronJob) {
        String localServletContainerSocketInfo = applicationStatus.getServletContainerSocketInfo();
        String lockName = getLockKeyName(cronJob);
        Expiration expiration = Expiration.seconds(cronJobLockExpirationTime);

        redisTemplate.execute((RedisCallback<String>) connection -> {
            connection.set(lockName.getBytes(), localServletContainerSocketInfo.getBytes(), expiration, SET_IF_ABSENT);
            return null;
        });

        Object value = redisTemplate.opsForValue().get(lockName);
        String lockHolderAppSocketInfo = value == null ? "" : value.toString();

        return localServletContainerSocketInfo.equals(lockHolderAppSocketInfo);
    }

    /**
     * 删除锁
     *
     * @param cronJob 非阻塞式的获取任务对应的锁
     */
    public void deleteLock(CronJob cronJob) {
        String localServletContainerSocketInfo = applicationStatus.getServletContainerSocketInfo();
        String lockName = getLockKeyName(cronJob);
        String lockContent = redisTemplate.opsForValue().get(lockName);

        if (localServletContainerSocketInfo.equals(lockContent)) {
            redisTemplate.delete(lockName);
        } else {
            log.warn("尝试删除job关联的锁时不符合条件，不是当前应用创建的锁：期望值为{}，实际值为{}", localServletContainerSocketInfo, lockContent);
        }
    }

    /**
     * 为锁续上超时时间
     *
     * @param cronJob 作业
     */
    public void inspireLock(CronJob cronJob) {
        String lockKey = getLockKeyName(cronJob);

        Boolean result = redisTemplate.expire(lockKey, cronJobLockExpirationTime, TimeUnit.SECONDS);
        log.info("设置锁时间结果：{}->{}", lockKey, result);
    }

    /**
     * @param cronJobs 作业列表
     * @return 获取作业列表分别是否与锁关联
     */
    public Map<CronJob, Boolean> getStatusOfJobLockHoldInfo(List<CronJob> cronJobs) {
        if (CollectionUtils.isEmpty(cronJobs)) {
            return Collections.emptyMap();
        }

        List<String> cronJobLockHoldInfoList = redisTemplate.opsForValue().multiGet(cronJobs.stream().map(this::getLockKeyName)
                .collect(Collectors.toList()));

        if (CollectionUtils.isEmpty(cronJobLockHoldInfoList) || cronJobLockHoldInfoList.size() != cronJobs.size()) {
            throw new IllegalStateException("返回作业锁关联信息数量有误：期望" + cronJobs.size() + "，返回：" + CollectionUtils.size(cronJobLockHoldInfoList));
        }

        Map<CronJob, Boolean> statusOfJobLockHoldInfoMap = Maps.newHashMapWithExpectedSize(cronJobLockHoldInfoList.size());
        Iterator<String> cronJobLockIterator = cronJobLockHoldInfoList.iterator();
        cronJobs.forEach(cronJob -> statusOfJobLockHoldInfoMap.put(cronJob, cronJobLockIterator.next() != null));

        return statusOfJobLockHoldInfoMap;
    }

    private String getLockKeyName(CronJob cronJob) {
        return CronTaskKeyConstants.JOB_LOCK_PREFIX + CronTaskKeyConstants.SEPERATOR +
                cronJob.getTaskName() + CronTaskKeyConstants.SEPERATOR +
                formatDate(cronJob.getStartTime());
    }

    private String getProcessListKeyName(String taskName) {
        return CronTaskKeyConstants.PROCESS_STATUS_LIST_PREFIX + CronTaskKeyConstants.SEPERATOR + taskName;
    }

    private boolean isJobCompleted(String statusText) {
        return JOB_STATUS_COMPLETED.equals(statusText);
    }

    private String formatDate(long millis) {
        return DateFormatUtils.format(millis, FORMAT_PATTERN);
    }

    private long parseDate(String date) {
        try {
            return dateParser.parse(date).getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }
}
