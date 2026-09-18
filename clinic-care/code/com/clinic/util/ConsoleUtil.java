package com.clinic.util;

import java.util.Scanner;

/**
 * All console input and output goes through this class, so the look of the
 * application can be changed in one place.
 *
 * Syllabus concepts: Scanner input, System.out formatting, static methods,
 * String repeat / formatting, varargs.
 */
public final class ConsoleUtil {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final int WIDTH = 74;

    private ConsoleUtil() {
    }

    public static void line() {
        System.out.println(repeat('-', WIDTH));
    }

    public static void doubleLine() {
        System.out.println(repeat('=', WIDTH));
    }

    public static void header(String title) {
        System.out.println();
        doubleLine();
        System.out.println("  " + title.toUpperCase());
        doubleLine();
    }

    public static void subHeader(String title) {
        System.out.println();
        System.out.println("  " + title);
        line();
    }

    public static void info(String message) {
        System.out.println("  " + message);
    }

    public static void success(String message) {
        System.out.println("  [OK]  " + message);
    }

    public static void warn(String message) {
        System.out.println("  [!]   " + message);
    }

    public static void error(String message) {
        System.out.println("  [ERR] " + message);
    }

    public static void blank() {
        System.out.println();
    }

    /** Reads one line of input after printing a prompt. */
    public static String ask(String prompt) {
        System.out.print("  " + prompt + ": ");
        if (!SCANNER.hasNextLine()) {
            return "";     // input stream closed (piped input ran out)
        }
        return SCANNER.nextLine();
    }

    public static void pause() {
        System.out.print("  Press ENTER to continue...");
        if (SCANNER.hasNextLine()) {
            SCANNER.nextLine();
        }
    }

    public static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /** Prints a labelled row, e.g.  Name            : Ramesh Kumar */
    public static void field(String label, Object value) {
        System.out.printf("  %-18s: %s%n", label, value);
    }
}
