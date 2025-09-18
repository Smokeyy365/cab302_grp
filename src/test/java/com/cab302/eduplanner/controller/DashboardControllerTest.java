package com.cab302.eduplanner.controller;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DashboardController class.
 *
 * These tests verify that:
 * 1. The sort mode cycles through all values in the expected order.
 * 2. A tempTask object can be created and its fields behave as expected.
 */
class DashboardControllerTest {

    // --- helpers for reflection ---
    private static Object get(Object target, String field) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invokeNoArg(Object target, String method) throws Exception {
        Method m = target.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(target);
    }

    @Test
    void sortModeCyclesThroughAllValues() throws Exception {
        DashboardController controller = new DashboardController();

        // Default sort mode
        assertEquals("DUE_DATE", ((Enum<?>) get(controller, "sortMode")).name());

        invokeNoArg(controller, "cycleSort");
        assertEquals("ALPHA", ((Enum<?>) get(controller, "sortMode")).name());

        invokeNoArg(controller, "cycleSort");
        assertEquals("GROUPED_SUBJECT", ((Enum<?>) get(controller, "sortMode")).name());

        invokeNoArg(controller, "cycleSort");
        assertEquals("DUE_DATE", ((Enum<?>) get(controller, "sortMode")).name());
    }

    @Test
    void tempTaskCanBeCreatedAndArchived() throws Exception {
        // Locate the inner tempTask class
        Class<?> tempTaskClass = null;
        for (Class<?> cls : DashboardController.class.getDeclaredClasses()) {
            if (cls.getSimpleName().equals("tempTask")) {
                tempTaskClass = cls;
                break;
            }
        }
        assertNotNull(tempTaskClass);

        // Create a new tempTask
        Constructor<?> ctor = tempTaskClass.getDeclaredConstructor(String.class, String.class, LocalDate.class);
        ctor.setAccessible(true);
        LocalDate due = LocalDate.now().plusDays(1);
        Object temp = ctor.newInstance("Homework", "CAB302", due);

        // Access fields
        Field titleF = tempTaskClass.getDeclaredField("title");
        Field subjectF = tempTaskClass.getDeclaredField("subject");
        Field dueF = tempTaskClass.getDeclaredField("due");
        Field archivedF = tempTaskClass.getDeclaredField("archived");
        titleF.setAccessible(true);
        subjectF.setAccessible(true);
        dueF.setAccessible(true);
        archivedF.setAccessible(true);

        assertEquals("Homework", titleF.get(temp));
        assertEquals("CAB302", subjectF.get(temp));
        assertEquals(due, dueF.get(temp));
        assertFalse((boolean) archivedF.get(temp));

        // Change archived flag
        archivedF.set(temp, true);
        assertTrue((boolean) archivedF.get(temp));
    }
}
