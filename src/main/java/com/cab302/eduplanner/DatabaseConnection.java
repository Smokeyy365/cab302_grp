package com.cab302.eduplanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    // SQLite DB file (created in project root if not present)
    private static final String DB_URL = "jdbc:sqlite:eduplanner_database.db";

    public static void main(String[] args) {
        Connection connection = null;
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Open connection
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connection to SQLite has been established.");

            // Ensure foreign keys are enforced
            enableForeignKeys(connection);

            // Create schema if needed
            initSchema(connection);
            System.out.println("Database schema initialized (tables created if missing).");

        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                    System.out.println("Connection to SQLite closed.");
                }
            } catch (SQLException ex) {
                System.err.println("Error closing connection: " + ex.getMessage());
            }
        }
    }

    private static void enableForeignKeys(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
    }

    /**
     * Creates the Users, TaskTable, and RubricTable tables if they do not already exist.
     * Column names and types match your spec. Timestamps use SQLite's datetime('now') default.
     */
    private static void initSchema(Connection conn) throws SQLException {
        // USERS
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

        // TASKTABLE
        // Note: achieved_mark and weighted_mark use REAL to allow decimals; max_mark/task_weight are INTEGER.
        // deadline stored as TEXT (ISO string recommended) for portability in SQLite.
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

        // RUBRICTABLE
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

        // Helpful indexes on FKs
        final String createIdxTaskUser = """
            CREATE INDEX IF NOT EXISTS idx_tasktable_userid ON TaskTable(userid);
        """;
        final String createIdxRubricTask = """
            CREATE INDEX IF NOT EXISTS idx_rubric_taskid ON RubricTable(taskID);
        """;

        try (Statement st = conn.createStatement()) {
            st.execute(createUsers);
            st.execute(createTaskTable);
            st.execute(createRubricTable);
            st.execute(createIdxTaskUser);
            st.execute(createIdxRubricTask);
        }
    }
}
