package com.cab302.eduplanner.repository;

import com.cab302.eduplanner.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UserRepository {

    // Removed local DB_URL and init(); DatabaseConnection owns both

    public UserRepository() {
        // No schema creation here anymore
    }

    private static final DateTimeFormatter SQLITE_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // matches datetime

    public boolean createUser(String username, String email, String firstName, String lastName, String passwordHash) {
        final String sql = "INSERT INTO users (username, email, first_name, last_name, password_hash) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, passwordHash);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // Likely UNIQUE constraint if username/email exists
            return false;
        }
    }

    public Optional<User> findByUsername(String username) {
        final String sql = "SELECT user_id, username, email, first_name, last_name, password_hash, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
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

    // optional, not currently used
    public Optional<User> findByUserId(long userId) {
        final String sql = "SELECT user_id, username, email, first_name, last_name, password_hash, created_at FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
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
    public boolean updateDetails(long userId, String email, String firstName, String lastName) {
        final String sql = "UPDATE users SET email = ?, first_name = ?, last_name = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setLong(4, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            return false;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        long userId = rs.getLong("user_id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        String passwordHash = rs.getString("password_hash");

        // created_at is TEXT (strftime)
        String createdRaw = rs.getString("created_at");
        LocalDateTime createdAt = null;
        if (createdRaw != null && !createdRaw.isBlank()) {
            try {
                createdAt = LocalDateTime.parse(createdRaw, SQLITE_DT);
            } catch (Exception ignore) {
                // keeps null instead of fail
            }
        }

        return new User(userId, username, email, firstName, lastName, passwordHash, createdAt);
    }

    public static class User {
        private final long userid;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String passwordHash;
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
