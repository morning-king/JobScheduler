package com.cn.dmd.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * 日志工具类
 *
 * @author morningking
 * @since 2018/4/13 17:46
 */
public class LogUtil {
    public static void trace(Logger logger, String msg) {
        if (logger.isTraceEnabled()) {
            logger.trace(msg);
        }
    }

    public static void trace(Logger logger, String format, Object arg) {
        if (logger.isTraceEnabled()) {
            logger.trace(format, arg);
        }
    }

    public static void trace(Logger logger, String format, Object arg1, Object arg2) {
        if (logger.isTraceEnabled()) {
            logger.trace(format, arg1, arg2);
        }
    }

    public static void trace(Logger logger, String format, Object... arguments) {
        if (logger.isTraceEnabled()) {
            convert(arguments);
            logger.trace(format, arguments);
        }
    }

    public static void debug(Logger logger, String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }
    }

    public static void debug(Logger logger, String format, Object arg) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arg);
        }
    }

    public static void debug(Logger logger, String format, Object arg1, Object arg2) {
        if (logger.isDebugEnabled()) {
            logger.debug(format, arg1, arg2);
        }
    }

    public static void debug(Logger logger, String format, Object... arguments) {
        if (logger.isDebugEnabled()) {
            convert(arguments);
            logger.debug(format, arguments);
        }
    }

    public static void debug(Logger logger, String msg, Throwable t) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg, t);
        }
    }

    public static void info(Logger logger, String msg) {
        if (logger.isInfoEnabled()) {
            logger.info(msg);
        }
    }

    public static void info(Logger logger, String format, Object arg) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arg);
        }
    }

    public static void info(Logger logger, String format, Object arg1, Object arg2) {
        if (logger.isInfoEnabled()) {
            logger.info(format, arg1, arg2);
        }
    }

    public static void info(Logger logger, String format, Object... arguments) {
        if (logger.isInfoEnabled()) {
            convert(arguments);
            logger.info(format, arguments);
        }
    }

    public static void info(Logger logger, String msg, Throwable t) {
        if (logger.isInfoEnabled()) {
            logger.info(msg, t);
        }
    }

    public static void warn(Logger logger, String msg) {
        if (logger.isErrorEnabled()) {
            logger.warn(msg);
        }
    }

    public static void warn(Logger logger, String format, Object arg) {
        if (logger.isErrorEnabled()) {
            logger.warn(format, arg);
        }
    }

    public static void warn(Logger logger, String format, Object arg1, Object arg2) {
        if (logger.isErrorEnabled()) {
            logger.warn(format, arg1, arg2);
        }
    }

    public static void warn(Logger logger, String format, Object... arguments) {
        if (logger.isErrorEnabled()) {
            convert(arguments);
            logger.warn(format, arguments);
        }
    }

    public static void warn(Logger logger, String msg, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.warn(msg, t);
        }
    }

    public static void error(Logger logger, String msg) {
        if (logger.isErrorEnabled()) {
            logger.error(msg);
        }
    }

    public static void error(Logger logger, String format, Object arg) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arg);
        }
    }

    public static void error(Logger logger, String format, Object arg1, Object arg2) {
        if (logger.isErrorEnabled()) {
            logger.error(format, arg1, arg2);
        }
    }

    public static void error(Logger logger, String format, Object... arguments) {
        if (logger.isErrorEnabled()) {
            convert(arguments);
            logger.error(format, arguments);
        }
    }

    public static void error(Logger logger, String msg, Throwable t) {
        if (logger.isErrorEnabled()) {
            logger.error(msg, t);
        }
    }

    private static void convert(Object[] args) {
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && args[i] instanceof Supplier) {
                    args[i] = ((Supplier) (args[i])).get();
                }
            }
        }
    }
}
