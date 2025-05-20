/**
 * Photohub ---- To View Some S3xy Photos
 * Copyright (C) 2025 Loraine K. Cheung
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
