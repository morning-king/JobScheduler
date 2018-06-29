package com.cn.dmd.domain;

import com.cn.dmd.CronTask;
import com.cn.dmd.utils.DateUtils;
import com.cn.dmd.utils.JsonUtil;
import com.google.common.collect.Maps;
import lombok.EqualsAndHashCode;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定时作业，包含任务{@link CronTask}的动态层面【时间信息】
 *
 * @author morningking
 * @date 2017/7/12 16:09
 * @contact 243717042@qq.com
 */
@EqualsAndHashCode(of = {"taskName", "startTime"})
public class CronJob {
    //当前作业关联业务的开始时刻
    private final long startTime;
    //当前作业关联业务的结束时刻
    private final long endTime;
    //任务名称标识
    private final String taskName;

    public CronJob(String taskName, long startTime, long endTime) {
        this.taskName = taskName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getTaskName() {
        return taskName;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public LocalDateTime getStartTimeUsingLocalDateTime() {
        return new Timestamp(startTime).toLocalDateTime();
    }

    public LocalDateTime getEndTimeUsingLocalDateTime() {
        return new Timestamp(endTime).toLocalDateTime();
    }

    @Override
    public String toString() {
        Map<String, Object> paramsMap = Maps.newHashMapWithExpectedSize(3);
        paramsMap.put("taskName", taskName);
        paramsMap.put("startTime", DateUtils.formateDateStr(getStartTimeUsingLocalDateTime()));
        paramsMap.put("endTime", DateUtils.formateDateStr(getEndTimeUsingLocalDateTime()));

        return JsonUtil.toJsonString(paramsMap);
    }
}
