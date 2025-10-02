package com.cab302.eduplanner.service;

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

class AuthServiceRegisterResultTest {

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
    void registerWithResultReportsUsernameAndEmailTaken() {
        AuthService auth = new AuthService();
        String base = "user_" + UUID.randomUUID();
        String uname = base;
        String email = base + "@example.com";

        AuthService.RegisterResult r = auth.registerWithResult(uname, email, "F", "L", "pass");
        assertEquals(AuthService.RegisterResult.SUCCESS, r);
        createdUsernames.add(uname);

        // Attempt to register same username
        AuthService.RegisterResult r2 = auth.registerWithResult(uname, base + "2@example.com", "F", "L", "pass");
        assertEquals(AuthService.RegisterResult.USERNAME_TAKEN, r2);

        // Attempt to register same email (new username)
        AuthService.RegisterResult r3 = auth.registerWithResult(uname + "2", email, "F", "L", "pass");
        assertEquals(AuthService.RegisterResult.EMAIL_TAKEN, r3);
    }
}
