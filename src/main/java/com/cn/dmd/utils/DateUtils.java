package com.cn.dmd.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * 日期工具类
 *
 * @author morningking
 * @date 2017/7/12 16:09
 * @contact 243717042@qq.com
 */
@Slf4j
public class DateUtils {
    /**
     * @param date yyyy-MM-dd HH24:mm:ss
     * @return Date yyyy年MM月dd日 HH24:mm:ss
     */
    public static String formateDateStr(LocalDateTime date) {
        if (date == null) {
            return "";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
            return date.format(formatter);
        }
    }
}
