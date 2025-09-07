package com.cab302.eduplanner.repository; public class UserRepository {
    public static class User {
        final private String username;

        public User(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }
}