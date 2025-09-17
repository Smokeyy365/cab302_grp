package com.cab302.eduplanner.integration.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GoogleAuthClient {

    // Cache tokens per app user under ./tokens/<appUserId>/
    private static final Path TOKENS_DIR = Path.of("tokens");

    // Scopes required by the app
    private static final List<String> SCOPES = List.of(
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/drive.file"
    );

    /**
     * credentials.json must be at src/main/resources/credentials.json
     */
    public Credential getCredential(String appUserId) {
        try {
            if (!Files.exists(TOKENS_DIR)) {
                Files.createDirectories(TOKENS_DIR);
            }

            InputStream in = GoogleAuthClient.class
                    .getClassLoader()
                    .getResourceAsStream("credentials.json");
            if (in == null) {
                throw new IllegalStateException("Missing credentials.json in src/main/resources/");
            }

            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    GsonFactory.getDefaultInstance(),
                    new InputStreamReader(in)
            );

            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientSecrets,
                    SCOPES
            )
                    .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIR.resolve(appUserId).toFile()))
                    .setAccessType("offline")
                    .build();

            var receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(appUserId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create Google Credential", e);
        }
    }
}
