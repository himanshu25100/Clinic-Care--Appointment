package com.clinic.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A small developer tool reachable from the System Info menu. It prints the
 * structure of any model class at runtime without that class knowing about it.
 *
 * This is genuinely useful here: it is how the "Data Dictionary" screen is
 * generated, so the documentation can never drift out of sync with the code.
 *
 * Syllabus concept: Java Reflection.
 */
public final class ReflectionInspector {

    private ReflectionInspector() {
    }

    /** Prints fields and public methods of a class, discovered at runtime. */
    public static void describe(String className) {
        try {
            Class<?> clazz = Class.forName(className);

            ConsoleUtil.subHeader("Class: " + clazz.getSimpleName()
                    + "   (extends " + clazz.getSuperclass().getSimpleName() + ")");

            ConsoleUtil.info("Fields:");
            Field[] fields = clazz.getDeclaredFields();
            for (Field f : fields) {
                String mods = Modifier.toString(f.getModifiers());
                System.out.printf("    %-28s %-22s %s%n",
                        f.getName(), f.getType().getSimpleName(), mods);
            }

            ConsoleUtil.info("Public methods declared here:");
            Method[] methods = clazz.getDeclaredMethods();
            int shown = 0;
            for (Method m : methods) {
                if (Modifier.isPublic(m.getModifiers())) {
                    System.out.printf("    %-28s returns %s%n",
                            m.getName() + "()", m.getReturnType().getSimpleName());
                    shown++;
                }
                if (shown >= 12) {
                    ConsoleUtil.info("    ... (list truncated)");
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            ConsoleUtil.error("Class not found: " + className);
        }
    }
}
