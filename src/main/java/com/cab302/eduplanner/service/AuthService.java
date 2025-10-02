package com.cab302.eduplanner.service;

import com.cab302.eduplanner.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

// CHANGE: Use UserSession as the single source of truth for logged-in state.
import com.cab302.eduplanner.appcontext.UserSession; // CHANGE: new import

public class AuthService {
    private final UserRepository repo = new UserRepository();

    public enum RegisterResult {
        SUCCESS,
        INVALID_INPUT,
        USERNAME_TAKEN,
        EMAIL_TAKEN,
        INTERNAL_ERROR
    }

    // Method can only have one thread at a time
    public synchronized boolean register(String username, String email, String firstName, String lastName, String password) {
        return registerWithResult(username, email, firstName, lastName, password) == RegisterResult.SUCCESS;
    }

    public synchronized RegisterResult registerWithResult(String username, String email, String firstName, String lastName, String password) {
        if (username == null || email == null || firstName == null || lastName == null || password == null ||
                username.isBlank() || email.isBlank() || firstName.isBlank() || lastName.isBlank() || password.isBlank()) {
            return RegisterResult.INVALID_INPUT;
        }

        try {
            if (repo.existsByUsername(username)) return RegisterResult.USERNAME_TAKEN;
            if (repo.existsByEmail(email)) return RegisterResult.EMAIL_TAKEN;

            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            try {
                boolean created = repo.createUserOrThrow(username, email, firstName, lastName, hash);
                return created ? RegisterResult.SUCCESS : RegisterResult.INTERNAL_ERROR;
            } catch (UserRepository.UserCreationException ex) {
                String field = ex.getConstraintField();
                if ("username".equals(field)) return RegisterResult.USERNAME_TAKEN;
                if ("email".equals(field)) return RegisterResult.EMAIL_TAKEN;
                return RegisterResult.INTERNAL_ERROR;
            }
        } catch (Exception e) {
            return RegisterResult.INTERNAL_ERROR;
        }
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
