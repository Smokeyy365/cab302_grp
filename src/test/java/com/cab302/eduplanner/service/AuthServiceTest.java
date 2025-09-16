package com.cab302.eduplanner.service;

import com.cab302.eduplanner.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private static final String DB_URL = "jdbc:sqlite:eduplanner_database.db";
    private final List<String> createdUsernames = new ArrayList<>();

    @AfterEach
    void cleanUpUsers() throws SQLException {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {
            for (String username : createdUsernames) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
        }
        createdUsernames.clear();
    }

    @Test
    void registerRejectsBlankFields() {
        AuthService authService = new AuthService();

        assertFalse(authService.register("", "user@example.com", "First", "Last", "password123"));
        assertFalse(authService.register("user", "", "First", "Last", "password123"));
        assertFalse(authService.register("user", "user@example.com", "", "Last", "password123"));
        assertFalse(authService.register("user", "user@example.com", "First", "", "password123"));
        assertFalse(authService.register("user", "user@example.com", "First", "Last", ""));
    }

    @Test
    void authenticateStoresCurrentUserWithoutSensitiveData() {
        AuthService authService = new AuthService();
        String username = "user_" + UUID.randomUUID();
        String email = username + "@example.com";
        String password = "secret123";

        boolean registered = authService.register(username, email, "First", "Last", password);
        if (registered) {
            createdUsernames.add(username);
        }
        assertTrue(registered, "Registration should succeed for fresh user");

        assertTrue(authService.authenticate(username, password), "Authentication should succeed with correct password");

        UserRepository.User currentUser = authService.getCurrentUser();
        assertNotNull(currentUser, "Current user should be set after successful auth");
        assertEquals(username, currentUser.getUsername());
        assertNull(currentUser.getPasswordHash(), "Sensitive password hash should be stripped");
    }
}
