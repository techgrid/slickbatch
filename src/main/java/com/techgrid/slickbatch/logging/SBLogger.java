package com.techgrid.slickbatch.logging;

public class SBLogger {
    public static void info(String string) {
        System.out.println("\u001B[34m" + string.replaceAll("\n", "\n\u001B[34m") + "\u001B[37m");
    }

    public static void success(String string) {
        System.out.println("\u001B[32m" + string.replaceAll("\n", "\n\u001B[32m") + "\u001B[37m");
    }

    public static void error(String string) {
        System.out.println("\u001B[31m" + string.replaceAll("\n", "\n\u001B[31m") + "\u001B[37m");
    }

    public static void warn(String string) {
        System.out.println("\u001B[33m" + string.replaceAll("\n", "\n\u001B[33m") + "\u001B[37m");
    }
}
