package com.cab302.eduplanner.appcontext;

import com.cab302.eduplanner.repository.UserRepository;

public final class UserSession {
    private static UserRepository.User currentUser;

    private UserSession() {}

    public static void setCurrentUser(UserRepository.User user) {
        currentUser = user;
    }

    public static UserRepository.User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}
