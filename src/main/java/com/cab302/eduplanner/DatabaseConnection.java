package com.cab302.eduplanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public static void main(String[] args) {
        Connection connection = null;
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Database URL: jdbc:sqlite:path/to/your/database.db
            // If the database file does not exist, it will be created. (Was created on first run)
            String url = "jdbc:sqlite:cab302_database.db";
            connection = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

        }

        catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }

        catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        }

        finally {
            try {
                if (connection != null) {
                    connection.close();
                    System.out.println("Connection to SQLite closed.");
                }
            }

            catch (SQLException ex) {
                System.err.println("Error closing connection: " + ex.getMessage());
            }
        }
    }
}
