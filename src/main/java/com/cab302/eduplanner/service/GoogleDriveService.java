package com.cab302.eduplanner.service;

import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Stores file in local google drive app*/
public class GoogleDriveService {

    private static final String prefNode = "eduplanner/drive";
    private static final String PrefDriveLocation = "driveBaseDir"; // <My Drive>/EduPlanner
    private final Preferences prefs = Preferences.userRoot().node(prefNode);

    public File getSavedDriveFolder() {
        String path = prefs.get(PrefDriveLocation, null);
        return (path != null) ? new File(path) : null;
    }

    public void saveDriveFolder(File dir) {
        if (dir != null) prefs.put(PrefDriveLocation, dir.getAbsolutePath());
    }

    public void clearSavedDriveFolder() {
        prefs.remove(PrefDriveLocation);
    }

    /** Try to find Google Drive locally  */
    public File guessDriveFolder() {
        String user = System.getProperty("user.name");

        // Stream my drive
        for (char letter = 'D'; letter <= 'L'; letter++) {
            File root = new File(letter + ":\\");
            File myDrive = new File(root, "My Drive");
            if (myDrive.isDirectory()) return myDrive;
            File alt = new File(root, "Google Drive");
            if (alt.isDirectory()) return alt;
        }

        // All possible paths
        List<File> candidates = new ArrayList<>();
        candidates.add(new File("C:\\Users\\" + user + "\\Google Drive"));
        candidates.add(new File("C:\\Users\\" + user + "\\Desktop\\Google Drive"));
        candidates.add(new File("/Users/" + user + "/Library/CloudStorage/GoogleDrive-" + user + "@gmail.com/My Drive"));
        candidates.add(new File("/Users/" + user + "/Google Drive"));

        for (File f : candidates) if (f.isDirectory()) return f;
        return null;
    }

    /** Ask user for Drive location ensure Edu planner folder has path  */
    public File pickAndSetupEduPlanner(Window ownerWindow) {
        File initial = guessDriveFolder();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose your Google Drive folder (the one that syncs)");

        if (initial != null && initial.isDirectory()) {
            File start = initial.getParentFile() != null ? initial.getParentFile() : initial;
            if (start.isDirectory()) try { chooser.setInitialDirectory(start); } catch (Exception ignored) {}
        } else {
            File home = new File(System.getProperty("user.home"));
            if (home.isDirectory()) try { chooser.setInitialDirectory(home); } catch (Exception ignored) {}
        }

        File picked = chooser.showDialog(ownerWindow);
        if (picked == null) return null;

        File myDrive = new File(picked, "My Drive");
        File base = myDrive.isDirectory() ? myDrive : picked;

        File eduPlanner = new File(base, "EduPlanner");
        File notes = new File(eduPlanner, "Notes");
        File flashcards = new File(eduPlanner, "Flashcards");

        if (!eduPlanner.exists()) eduPlanner.mkdirs();
        if (!notes.exists()) notes.mkdirs();
        if (!flashcards.exists()) flashcards.mkdirs();

        saveDriveFolder(eduPlanner);
        return eduPlanner;
    }

    /** Ensure subfolder is included  */
    public File ensureSubfolder(String subfolderName) {
        File base = getSavedDriveFolder();
        if (base == null || !base.isDirectory()) return null;
        File sub = new File(base, subfolderName);
        if (!sub.exists()) sub.mkdirs();
        return sub;
    }
}
