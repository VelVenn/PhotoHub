package io.loraine.photohub.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSS");

    private static final boolean THREAD_INFO = true;

    private static String getThreadInfo() {
        if (THREAD_INFO) {
            StringJoiner joiner = new StringJoiner(" | ", " [ ", " ] ");
            return joiner.add(Thread.currentThread().getName())
                    .add(String.valueOf(Thread.currentThread().threadId()))
                    .add(String.valueOf(Thread.currentThread().getState()))
                    .toString();
        }
        return "";
    }

    public static void log(String msg) {
        String time = LocalDateTime.now().format(formatter);
        time = "[" + time + "] ";
        System.out.println(time + msg + getThreadInfo());
    }

    public static void log(String msg, Object... args) {
        String time = LocalDateTime.now().format(formatter);
        time = "[" + time + "] ";
        System.out.printf(time + msg, args);
        System.out.print(getThreadInfo());
        System.out.println();
    }

    public static void logErr(String msg) {
        String time = LocalDateTime.now().format(formatter);
        time = "[" + time + "] ";
        System.err.println(time + msg + getThreadInfo());
    }

    public static void logErr(String msg, Object... args) {
        String time = LocalDateTime.now().format(formatter);
        time = "[" + time + "] ";
        System.err.printf(time + msg, args);
        System.err.print(getThreadInfo());
        System.err.println();
    }

    public static void logErr(String msg, Throwable e) {
        String time = LocalDateTime.now().format(formatter);
        time = "[" + time + "] ";

        if (e != null) {
            System.err.println(time + msg + e.getMessage() + " | Cause: " + e.getCause() + getThreadInfo());
        } else {
            System.err.println(time + msg + getThreadInfo());
        }
    }

    public static void main(String[] args) {
        log("Hello, World!");
        log("Hello, %s!", "World");
        logErr("Error: %s", "Something went wrong");
        logErr("Error: %s, Caused by: %s", "Something went wrong", new Exception("Test exception"));
        logErr("Error: ", new Exception("Test exception"));
    }
}
