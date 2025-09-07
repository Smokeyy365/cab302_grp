package com.cab302.eduplanner.service;

import com.cab302.eduplanner.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AuthService {
    private static final String STORE_FILENAME = "users.properties";
    // store is a properties file with username=hashed-password pairs
    private final Properties store = new Properties();
    private UserRepository.User currentUser;

    public AuthService() {
        loadStore();
    }

    private void loadStore() {
        Path p = Path.of(System.getProperty("user.dir")).resolve(STORE_FILENAME);
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                store.load(in);
            } catch (IOException ignored) {}
        }
    }

    private void saveStore() {
        Path p = Path.of(System.getProperty("user.dir")).resolve(STORE_FILENAME);
        try (OutputStream out = Files.newOutputStream(p)) {
            store.store(out, "User store (username=bcyrpt-hash)");
        } catch (IOException ignored) {}
    }

    // synchronized means only one thread can access this method at a time
    public synchronized boolean register(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) return false;
        if (store.containsKey(username)) return false;
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        store.setProperty(username, hash);
        saveStore();
        return true;
    }

    public synchronized boolean authenticate(String username, String password) {
        if (!store.containsKey(username)) return false;
        String storedHash = store.getProperty(username);
        boolean ok = BCrypt.checkpw(password, storedHash);
        if (ok) currentUser = new UserRepository.User(username);
        return ok;
    }

    public UserRepository.User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }
}

