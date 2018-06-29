package com.cn.dmd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

/**
 * @author morningking
 * @date 2017/7/13 18:00
 * @contact 243717042@qq.com
 */
public abstract class AbstractCronTask implements CronTask {
    @Autowired
    private CronTaskRegistry cronTaskRegistry;
    private volatile CronTaskConfig taskConfig;

    @PostConstruct
    private void autoRegister() {
        Assert.notNull(getTaskConfig(), "当前task没有配置taskConfig，task为：" + this.getClass().getName());
        cronTaskRegistry.register(getTaskConfig().getName(), this);
    }

    public CronTaskConfig getTaskConfig() {
        return taskConfig;
    }

    protected void setTaskConfig(CronTaskConfig cronTaskConfig) {
        this.taskConfig = cronTaskConfig;
    }
}
