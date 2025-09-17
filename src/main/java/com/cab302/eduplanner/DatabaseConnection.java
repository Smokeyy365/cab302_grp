package com.cab302.eduplanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // Single DB URL
    private static final String DB_URL = "jdbc:sqlite:eduplanner_database.db";

    // SQLite Driver
    static {
        try { Class.forName("org.sqlite.JDBC"); } catch (ClassNotFoundException ignored) {}
    }

    /** Enables foreign key once per connection */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /** Creates/ensures all tables & indexes exist. Call once at app startup. */
    public static void initSchema() {
        final String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                email         TEXT    UNIQUE,
                first_name    TEXT,
                last_name     TEXT,
                password_hash TEXT    NOT NULL,
                created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now')),
                updated_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now'))
            );
        """;

        final String createTasks = """
            CREATE TABLE IF NOT EXISTS tasks (
                task_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER NOT NULL,
                subject       TEXT,
                title         TEXT    NOT NULL,
                due_date      TEXT,                                '
                notes         TEXT,
                weight        INTEGER,
                achieved_mark REAL,
                max_mark      REAL,
                created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now')),
                updated_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%S','now')),
                CHECK (weight IS NULL OR weight >= 0),
                CHECK (achieved_mark IS NULL OR achieved_mark >= 0),
                CHECK (max_mark IS NULL OR max_mark > 0),
                FOREIGN KEY (user_id)
                    REFERENCES users(user_id)
                    ON DELETE CASCADE
            );
        """;

        final String createRubrics = """
            CREATE TABLE IF NOT EXISTS rubrics (
                rubric_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                task_id       INTEGER NOT NULL,
                location      TEXT,
                FOREIGN KEY (task_id)
                    REFERENCES tasks(task_id)
                    ON DELETE CASCADE
            );
        """;

        final String createIdxTasksUser = """
            CREATE INDEX IF NOT EXISTS idx_tasks_user_id  ON tasks(user_id);
        """;
        final String createIdxTasksDue = """
            CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date);
        """;
        final String createIdxRubricsTask = """
            CREATE INDEX IF NOT EXISTS idx_rubrics_task_id ON rubrics(task_id);
        """;

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(createUsers);
            st.execute(createTasks);
            st.execute(createRubrics);
            st.execute(createIdxTasksUser);
            st.execute(createIdxTasksDue);
            st.execute(createIdxRubricsTask);
        } catch (SQLException e) {
            System.err.println("Schema init failed: " + e.getMessage());
        }
    }

}
