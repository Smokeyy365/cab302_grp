package com.cab302.eduplanner.integration.google;

import com.google.api.services.drive.model.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Testing Menu

public class GoogleDriveTestMain {

    public static void main(String[] args) {

        System.out.println(" EDUPLANNER GOOGLE DRIVE TEST");


        try {
            // TEST 1: Connect to Google Drive
            System.out.println("Test 1 - Connect to google drive ");
            GoogleDriveService drive = new GoogleDriveService();
            System.out.println(" Connection successful \n");

            // TEST 2: List existing files
            System.out.println("TEST 2: Listing files ");
            List<File> files = drive.listFiles();

            if (files.isEmpty()) {
                System.out.println(" No files found ");
            } else {
                System.out.println(" Found " + files.size() + " files:");
                for (File file : files) {
                    System.out.println( file.getName() + " (ID: " + file.getId() + ")");
                }
            }
            System.out.println();

            // TEST 3: Upload a test file
            System.out.println("Test 3 Upload demo file");

            // Create a temporary test file
            Path tempFile = Files.createTempFile("eduplanner_test", ".txt");
            String content = "=== EDUPLANNER TEST FILE ===\n" +
                    "Created at: " + System.currentTimeMillis() + "\n" +
                    "This file was uploaded to test Google Drive integration.\n" +
                    "The text file works!!!";
            Files.writeString(tempFile, content);

            // Upload it
            String fileName = "EduPlanner_Test_" + System.currentTimeMillis() + ".txt";
            String fileId = drive.uploadFile(tempFile.toString(), fileName);

            System.out.println(" File uploaded successfully!");
            System.out.println(" File Name: " + fileName);
            System.out.println(" File ID: " + fileId);
            System.out.println("  View at: https://drive.google.com/file/d/" + fileId + "/view");

            // Clean up temporary file from computer
            Files.deleteIfExists(tempFile);
            System.out.println();

            // ALL TESTS PASSED!
            System.out.println("All tests have passed");
            System.out.println("\n Google Drive integration is working!");
            System.out.println("Check your Google Drive: https://drive.google.com");


        } catch (FileNotFoundException e) {
            // Credentials file is missing
            System.err.println("\n Cannot find credentials.json");


        } catch (Exception e) {
            //  if Some other error occurred
            System.err.println("\n TEST FAILED!");

        }
    }
}