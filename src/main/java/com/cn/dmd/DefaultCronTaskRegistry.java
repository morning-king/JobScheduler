package com.cn.dmd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author morningking
 * @date 2017/7/13 17:43
 * @contact 243717042@qq.com
 */
@Component
@Slf4j
public class DefaultCronTaskRegistry implements CronTaskRegistry {
    private final ConcurrentHashMap<String, CronTask> cronTaskMapping = new ConcurrentHashMap<>();

    @Override
    public CronTask getByTaskName(String taskName) {
        return cronTaskMapping.get(taskName);
    }

    @Override
    public void register(String taskName, CronTask cronTask) {
        cronTaskMapping.putIfAbsent(taskName, cronTask);
        log.debug("注册任务：{}->{}", taskName, cronTask.getClass().getName());
    }

    @Override
    public Set<String> getAllTaskNames() {
        return cronTaskMapping.keySet();
    }

    @PreDestroy
    private void onPreDestroy() {
        cronTaskMapping.clear();
    }

}
