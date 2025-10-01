package com.cab302.eduplanner.integration.google;

// Libraries for google drive integration
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;
import java.io.*;
import java.util.Collections;
import java.util.List;


// Helps connect to google drive
public class GoogleDriveService {

    // App title display
    private static final String appTitle = "EduPlanner";

    // Converts between json and java
    private static final JsonFactory jsonConverter = GsonFactory.getDefaultInstance();

    //Save log in path
    private static final String tokens = "tokens";

    //Permissions asked
    //DRIVE_FIlE is the files created
    private static final List<String> scopes = Collections.singletonList(DriveScopes.DRIVE_FILE);

    //Google credentials file location
    private static final String credPath = "/credentials.json";

    // OAuth server port
    private static final int authPort = 8888;

    // Default page size for listing files
    private static final int pageSize = 10;

    // MIME type for binary files
    private static final String mimeType = "application/octet-stream";

    // Drive connection
    private Drive driveService;


    // Run when creating new Google Drive service
    //e.g  GoogleDriveService myDrive = new GoogleDriveService();
    public GoogleDriveService() throws Exception {

        // Set up the connection to Google Drive
        this.driveService = initializeDriveService();
    }



    //Gets Permission from google drive
    // NetHttpTransport is a secure
    private static Credential getCredentials(final NetHttpTransport transport) throws IOException {

        // Try to load the credentials.json file from credentials
        InputStream in = GoogleDriveService.class.getResourceAsStream(credPath);

        // If the file doesn't exist, throw an error
        if (in == null) {
            throw new FileNotFoundException("Credentials file not found: " + credPath);
        }

        // Read the credentials file
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonConverter, new InputStreamReader(in));

        // Create a process of getting permission
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport,        // How data is sent
                jsonConverter,    // How we format data
                clientSecrets,    // Clients profile
                scopes)           // Type of permission

                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokens)))
                .setAccessType("offline")
                .build();

        // Set up a local web server to receive the login response
        // Port 8888 is the local host
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(authPort).build();


        // Check if save tokens are present
        // open browser if not
        //Save tokens when we get it
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }


    private static Drive initializeDriveService() throws Exception {
        // Create a secure internet connection via HTTP
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();



        // Build and return the drive service using JSON formatting,permission credentials,secure connection  and app name

        return new Drive.Builder(httpTransport, jsonConverter, getCredentials(httpTransport))
                .setApplicationName(appTitle)
                .build();
    }

    // Create google file object  using new
    // Store in variable

    public String uploadFile(String filePath, String fileName) throws IOException {

        // Create "metadata" - info on file
        File fileMetadata = new File();
        fileMetadata.setName(fileName);  // name on drive

        //  Get the actual file from your pc
        java.io.File file = new java.io.File(filePath);

        //Wrap it in a format Google Drive understands
        FileContent mediaContent = new FileContent(mimeType, file);

        //Upload
        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name")
                .execute();

        // Return the file's unique ID
        return uploadedFile.getId();
    }

    // Displays files with IDs,names,date of creation  and has a maximum number of files displayed
// Via method chaining
    public List<File> listFiles() throws IOException {

        // Ask Google Drive for a list of files
        FileList result = driveService.files().list()
                .setPageSize(pageSize) // maximum files returned
                .setFields("files(id, name, createdTime)")
                .execute();

        // Return the list of files
        return result.getFiles();
    }


    public Drive getDriveService() {
        return driveService;
    }

    // Additional Methods

    //Download file from google drive
    public void downloadFile(String fileId, String savePath) throws IOException {
        OutputStream outputStream = new FileOutputStream(savePath);
        driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
        outputStream.close();
    }

    // Delete file from google drive
    public void deleteFile(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
    }
}