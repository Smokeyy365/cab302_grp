package com.cab302.eduplanner.controller;

import com.cab302.eduplanner.model.Task;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for DashboardController.
 * - Sort mode cycles correctly
 * - safeSubject / safeTitle handle null, blank, and trimming
 */
class DashboardControllerTest {

    // ---- tiny reflection helpers ----
    private static String sortModeName(DashboardController c) throws Exception {
        Field f = DashboardController.class.getDeclaredField("sortMode");
        f.setAccessible(true);
        return ((Enum<?>) f.get(c)).name();
    }

    private static void cycle(DashboardController c) throws Exception {
        Method m = DashboardController.class.getDeclaredMethod("cycleSort");
        m.setAccessible(true);
        m.invoke(c);
    }

    private static String callSafe(String method, Task t) throws Exception {
        Method m = DashboardController.class.getDeclaredMethod(method, Task.class);
        m.setAccessible(true);
        return (String) m.invoke(null, t); // static helper
    }

    // ---- sort mode tests (2) ----

    @Test
    void sortMode_startsAtDueDate_andCyclesToAlpha() throws Exception {
        DashboardController c = new DashboardController();
        assertEquals("DUE_DATE", sortModeName(c));
        cycle(c);
        assertEquals("ALPHA", sortModeName(c));
    }

    @Test
    void sortMode_cyclesBackToDueDate_afterThreeSteps() throws Exception {
        DashboardController c = new DashboardController();
        cycle(c); // DUE_DATE -> ALPHA
        cycle(c); // ALPHA -> GROUPED_SUBJECT
        cycle(c); // GROUPED_SUBJECT -> DUE_DATE
        assertEquals("DUE_DATE", sortModeName(c));
    }

    // ---- safeSubject tests (3) ----

    @Test
    void safeSubject_returnsPlaceholder_whenNull() throws Exception {
        Task t = new Task(1L, null, "T", null, null, null, null, null);
        assertEquals("(No Subject)", callSafe("safeSubject", t));
    }

    @Test
    void safeSubject_returnsPlaceholder_whenBlank() throws Exception {
        Task t = new Task(1L, "   ", "T", null, null, null, null, null);
        assertEquals("(No Subject)", callSafe("safeSubject", t));
    }

    @Test
    void safeSubject_trimsWhitespace() throws Exception {
        Task t = new Task(1L, "  CAB302  ", "T", null, null, null, null, null);
        assertEquals("CAB302", callSafe("safeSubject", t));
    }

    // ---- safeTitle tests (3) ----

    @Test
    void safeTitle_returnsPlaceholder_whenNull() throws Exception {
        Task t = new Task(1L, "S", null, null, null, null, null, null);
        assertEquals("(Untitled Task)", callSafe("safeTitle", t));
    }

    @Test
    void safeTitle_returnsPlaceholder_whenBlank() throws Exception {
        Task t = new Task(1L, "S", "   ", null, null, null, null, null);
        assertEquals("(Untitled Task)", callSafe("safeTitle", t));
    }

    @Test
    void safeTitle_trimsWhitespace() throws Exception {
        Task t = new Task(1L, "S", "  Report  ", null, null, null, null, null);
        assertEquals("Report", callSafe("safeTitle", t));
    }
}
