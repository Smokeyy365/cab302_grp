package com.cab302.eduplanner.service;

import com.cab302.eduplanner.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Handles user sign-up, sign-in, and session state backed by {@link UserRepository}.
 */
public class AuthService {
    private final UserRepository repo = new UserRepository();
    private UserRepository.User currentUser;

    // synchronized means only one thread can access this method at a time
    /**
     * Validates input and creates a user account with a hashed password.
     *
     * @param username unique username requested by the user
     * @param email contact email for the account
     * @param firstName given name used in greetings
     * @param lastName family name used in greetings
     * @param password plain text password to hash
     * @return {@code true} when the account is created successfully
     */
    public synchronized boolean register(String username, String email, String firstName, String lastName, String password) {
        if (username == null || email == null || firstName == null || lastName == null || password == null ||
                username.isBlank() || email.isBlank() || firstName.isBlank() || lastName.isBlank() || password.isBlank()) {
            return false;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return repo.createUser(username, email, firstName, lastName, hash);
    }

    /**
     * Checks the supplied credentials against the stored hash and records the session user.
     *
     * @param username account identifier to look up
     * @param password plain text password to verify
     * @return {@code true} when authentication succeeds
     */
    public synchronized boolean authenticate(String username, String password) {
        Optional<UserRepository.User> u = repo.findByUsername(username);
        if (u.isEmpty()) return false;
        String storedHash = u.get().getPasswordHash();
        if (storedHash == null || storedHash.isBlank()) return false;
        boolean ok = BCrypt.checkpw(password, storedHash);
        if (ok) currentUser = u.get().withoutSensitive();
        return ok;
    }

    /**
     * Returns the currently authenticated user, if any.
     *
     * @return authenticated user without sensitive fields, or {@code null} if no session exists
     */
    public UserRepository.User getCurrentUser() {
        return currentUser;
    }

    /**
     * Clears any active session.
     */
    public void logout() {
        currentUser = null;
    }
}

