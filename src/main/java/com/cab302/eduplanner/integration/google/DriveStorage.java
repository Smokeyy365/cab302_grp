package com.cab302.eduplanner.integration.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.util.List;

public class DriveStorage {

    private final Drive drive;
    private static final String appName = "EduPlanner";
    private static final String rootFolder = "EduPlanner";

    public DriveStorage(String appUserId) {
        try {
            var transportHttp = GoogleNetHttpTransport.newTrustedTransport();
            var credentials = new GoogleAuthClient().getCredential(appUserId);
            this.drive = new Drive.Builder(transportHttp, GsonFactory.getDefaultInstance(), credentials)
                    .setApplicationName(appName)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error while starting Google Drive service", e);
        }
    }

    /** Ensure an 'EduPlanner' folder exists; return its id. */
    private String ensureRootFolder() {
        try {
            String q = "mimeType='application/vnd.google-apps.folder' and name='" + rootFolder + "' and trashed=false";
            FileList list = drive.files().list()
                    .setQ(q)
                    .setSpaces("drive")
                    .setFields("files(id,name)")
                    .execute();

            if (!list.getFiles().isEmpty()) return list.getFiles().get(0).getId();

            var meta = new com.google.api.services.drive.model.File();
            meta.setName(rootFolder);
            meta.setMimeType("application/vnd.google-apps.folder");
            return drive.files().create(meta).setFields("id").execute().getId();
        } catch (Exception e) {
            throw new RuntimeException("ensureRootFolder failed", e);
        }
    }

    /** Upload a local file into the EduPlanner folder; returns Drive file id. */
    public String upload(java.io.File localFile, String mimeType) {
        try {
            String parentId = ensureRootFolder();

            var meta = new com.google.api.services.drive.model.File();
            meta.setName(localFile.getName());
            meta.setParents(List.of(parentId));

            var media = new FileContent(mimeType, localFile);
            return drive.files().create(meta, media).setFields("id").execute().getId();
        } catch (Exception e) {
            throw new RuntimeException("upload failed", e);
        }
    }
}
