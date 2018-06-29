package com.cn.dmd.constants;

/**
 * 定时任务 时间粒度
 *
 * @author morningking
 * @date 2017/7/14 15:40
 * @contact 243717042@qq.com
 */
public enum CronScheduleTimeUnit {
    MINUTE(0),
    HOUR(1),;

    private int unit;

    CronScheduleTimeUnit(int unit) {
        this.unit = unit;
    }

    public static CronScheduleTimeUnit parse(int type) {
        for (CronScheduleTimeUnit unit : values()) {
            if (unit.unit == type) {
                return unit;
            }
        }

        throw new IllegalArgumentException("错误的定时任务时间单位：" + type);
    }
}
