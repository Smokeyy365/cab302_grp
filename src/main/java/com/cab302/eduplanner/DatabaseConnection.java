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
            CREATE TABLE IF NOT EXISTS Users (
                userid        INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT    NOT NULL UNIQUE,
                password_hash TEXT    NOT NULL,
                email         TEXT,
                first_name    TEXT,
                last_name     TEXT,
                created_at    TEXT    NOT NULL DEFAULT (datetime('now'))
            );
        """;

        final String createTaskTable = """
            CREATE TABLE IF NOT EXISTS TaskTable (
                taskID        INTEGER PRIMARY KEY AUTOINCREMENT,
                userid        INTEGER NOT NULL,
                subject       TEXT,
                task_name     TEXT,
                deadline      TEXT,
                task_weight   INTEGER,
                achieved_mark REAL,
                max_mark      INTEGER,
                weighted_mark REAL,
                FOREIGN KEY (userid)
                    REFERENCES Users(userid)
                    ON UPDATE CASCADE
                    ON DELETE CASCADE
            );
        """;

        final String createRubricTable = """
            CREATE TABLE IF NOT EXISTS RubricTable (
                rubricID        INTEGER PRIMARY KEY AUTOINCREMENT,
                taskID          INTEGER NOT NULL,
                rubric_location TEXT,
                FOREIGN KEY (taskID)
                    REFERENCES TaskTable(taskID)
                    ON UPDATE CASCADE
                    ON DELETE CASCADE
            );
        """;

        final String createIdxTaskUser = """
            CREATE INDEX IF NOT EXISTS idx_tasktable_userid ON TaskTable(userid);
        """;
        final String createIdxRubricTask = """
            CREATE INDEX IF NOT EXISTS idx_rubric_taskid ON RubricTable(taskID);
        """;

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.execute(createUsers);
            st.execute(createTaskTable);
            st.execute(createRubricTable);
            st.execute(createIdxTaskUser);
            st.execute(createIdxRubricTask);
        } catch (SQLException e) {
            System.err.println("Schema init failed: " + e.getMessage());
        }
    }

}
