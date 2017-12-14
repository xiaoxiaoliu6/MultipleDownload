package com.ldw.eagle.multipledownload;

import android.text.TextUtils;
import android.util.Log;

public class LogUtils {
    private static final boolean isLog = true;
    private static final String DEFAULT_TAG = "callback";
    public static String customTagPrefix = "shouba_";
    public static boolean debug = BuildConfig.DEBUG;
    private static int maxLogSize = 3000;

    private static String generateTag(StackTraceElement caller) {
        String tag = "%s.%s(L:%d)";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName
                .lastIndexOf(".") + 1);
        tag = String.format(tag, callerClazzName, caller.getMethodName(),
                caller.getLineNumber());
        tag = TextUtils.isEmpty(customTagPrefix) ? tag : customTagPrefix + ":"
                + tag;
        return tag;
    }

    public static void d(String msg) {
        if (null == msg || "".equals(msg)) return;
        if (debug) {

            StackTraceElement caller = getCallerStackTraceElement();
            String tag = generateTag(caller);

            // 处理log 过长无法完整打印的问题
            for (int i = 0; i <= msg.length() / maxLogSize; i++) {

                int start = i * maxLogSize;

                int end = (i + 1) * maxLogSize;

                end = end > msg.length() ? msg.length() : end;

                Log.d(tag, msg.substring(start, end));

            }
        }
    }

    public static void d(String content, Throwable tr) {
        if (null == content || "".equals(content)) return;
        if (!debug)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.d(tag, content, tr);
    }

    private static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    public static void i(String msg) {
        if (null == msg || "".equals(msg)) return;
        if (debug) {

            StackTraceElement caller = getCallerStackTraceElement();
            String tag = generateTag(caller);

            for (int i = 0; i <= msg.length() / maxLogSize; i++) {

                int start = i * maxLogSize;

                int end = (i + 1) * maxLogSize;

                end = end > msg.length() ? msg.length() : end;

                Log.i(tag, msg.substring(start, end));

            }
        }
    }

    public static void i(String content, Throwable tr) {
        if (null == content || "".equals(content)) return;
        if (!debug)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.i(tag, content, tr);
    }

    public static void e(String msg) {
        if (null == msg || "".equals(msg)) return;
        if (debug) {

            StackTraceElement caller = getCallerStackTraceElement();
            String tag = generateTag(caller);

            for (int i = 0; i <= msg.length() / maxLogSize; i++) {

                int start = i * maxLogSize;

                int end = (i + 1) * maxLogSize;

                end = end > msg.length() ? msg.length() : end;

                Log.e(tag, msg.substring(start, end));

            }
        }
    }

    public static void e(String content, Throwable tr) {
        if (null == content || "".equals(content)) return;
        if (!debug)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
    }

    public static void e(String tag, String msg) {
        if (!debug) return;
        Log.e(tag, msg);
    }

    public static void log(String tag, int level, String msg, Throwable tr) {
        if (isLog) {
            switch (level) {
                case Log.VERBOSE:
                    if (tr == null) {
                        Log.v(tag, msg);
                    } else {
                        Log.v(tag, msg, tr);
                    }
                    break;
                case Log.INFO:
                    if (tr == null) {
                        Log.i(tag, msg);
                    } else {
                        Log.i(tag, msg, tr);
                    }
                    break;
                case Log.DEBUG:
                    if (tr == null) {
                        Log.d(tag, msg);
                    } else {
                        Log.d(tag, msg, tr);
                    }
                    break;
                case Log.WARN:
                    if (tr == null) {
                        Log.w(tag, msg);
                    } else {
                        Log.w(tag, msg, tr);
                    }
                    break;
                case Log.ERROR:
                    if (tr == null) {
                        Log.e(tag, msg, tr);
                    } else {
                        Log.e(tag, msg, tr);
                    }

                    break;
            }
        }

    }

    public static void log(String tag, int level, String msg) {
        if (isLog) {
            log(tag, level, msg, null);
        }

    }

    public static void log(String tag, String msg) {
        if (isLog) {
            log(DEFAULT_TAG, Log.INFO, tag + msg, null);
        }
    }

    public static void log(String msg) {
        if (isLog) {
            log(DEFAULT_TAG, Log.INFO, msg, null);
        }
    }

}
