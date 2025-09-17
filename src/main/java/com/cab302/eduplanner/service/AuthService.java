package com.cab302.eduplanner.service;

import com.cab302.eduplanner.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

// CHANGE: Use UserSession as the single source of truth for logged-in state.
import com.cab302.eduplanner.appcontext.UserSession; // CHANGE: new import

public class AuthService {
    private final UserRepository repo = new UserRepository();

    // Method can only have one thread at a time
    public synchronized boolean register(String username, String email, String firstName, String lastName, String password) {
        if (username == null || email == null || firstName == null || lastName == null || password == null ||
                username.isBlank() || email.isBlank() || firstName.isBlank() || lastName.isBlank() || password.isBlank()) {
            return false;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        return repo.createUser(username, email, firstName, lastName, hash);
    }

    public synchronized boolean authenticate(String username, String password) {
        Optional<UserRepository.User> u = repo.findByUsername(username);
        if (u.isEmpty()) return false;
        String storedHash = u.get().getPasswordHash();
        if (storedHash == null || storedHash.isBlank()) return false;
        boolean ok = BCrypt.checkpw(password, storedHash);
        if (ok) {
            // Removed local state
            UserSession.setCurrentUser(u.get().withoutSensitive());
        }
        return ok;
    }

    public UserRepository.User getCurrentUser() {
        return UserSession.getCurrentUser();
    }

    public void logout() {
        UserSession.clear();
    }
}
