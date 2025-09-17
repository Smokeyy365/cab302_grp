package com.cab302.eduplanner.repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Provides CRUD operations for application users backed by a local SQLite database.
 */
public class UserRepository {
    private static final String DB_URL = "jdbc:sqlite:eduplanner_database.db";

    public UserRepository() {
        init();
    }

    private void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {}
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "userid INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "email TEXT, " +
                "first_name TEXT, " +
                "last_name TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        } catch (SQLException e) {
            // In production, use a logger
            System.err.println("Failed to initialize users table: " + e.getMessage());
        }
    }

    /**
     * Persists a new user record if the username is still available.
     *
     * @param username unique username
     * @param email email address to store
     * @param firstName given name
     * @param lastName family name
     * @param passwordHash bcrypt hash for the password
     * @return {@code true} when one row is inserted
     */
    public boolean createUser(String username, String email, String firstName, String lastName, String passwordHash) {
        String sql = "INSERT INTO users (username, email, first_name, last_name, password_hash) VALUES (?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, passwordHash);
            int updated = ps.executeUpdate();
            return updated == 1;
        } catch (SQLException e) {
            // Likely UNIQUE constraint if username exists
            return false;
        }
    }

    /**
     * Looks up a user by username.
     *
     * @param username account identifier
     * @return optional containing the matching user when found
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT userid, username, email, first_name, last_name, password_hash, created_at FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    // this is an optional method, not currently used
    /**
     * Retrieves a user by the database-generated identifier.
     *
     * @param userid primary key value
     * @return optional containing the matching user when found
     */
    public Optional<User> findByUserId(long userid) {
        String sql = "SELECT userid, username, email, first_name, last_name, password_hash, created_at FROM users WHERE userid = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    // TODO: implement updateDetails in AuthService and call this method
    /**
     * Updates non-sensitive profile details for an existing user.
     *
     * @param userid identifier of the user to update
     * @param email new email address
     * @param firstName new given name
     * @param lastName new family name
     * @return {@code true} when a row was updated
     */
    public boolean updateDetails(long userid, String email, String firstName, String lastName) {
        String sql = "UPDATE users SET email = ?, first_name = ?, last_name = ? WHERE userid = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setLong(4, userid);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Maps the current row of a {@link ResultSet} to the repository user model.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        long userid = rs.getLong("userid");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String passwordHash = rs.getString("password_hash");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        return new User(userid, username, email, firstName, lastName, passwordHash, createdAt);
    }

    /**
     * Immutable projection of a row in the users table.
     */
    public static class User {
        private final long userid;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String passwordHash; // may be null when exposed outside auth
        private final LocalDateTime createdAt;

        public User(long userid, String username, String email, String firstName, String lastName, String passwordHash, LocalDateTime createdAt) {
            this.userid = userid;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.passwordHash = passwordHash;
            this.createdAt = createdAt;
        }

        // Minimal constructor (e.g., testing)
        public User(String username) {
            this(-1L, username, null, null, null, null, null);
        }

        /**
         * Returns a copy of the user record without the password hash.
         */
        public User withoutSensitive() {
            return new User(userid, username, email, firstName, lastName, null, createdAt);
        }

        public long getUserId() { return userid; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPasswordHash() { return passwordHash; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
