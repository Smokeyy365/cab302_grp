package com.cab302.eduplanner.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NoteExportService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final float FONT_SIZE = 11f;
    private static final float LINE_HEIGHT = 14f;
    private static final float TITLE_SIZE = 16f;

    // PDFBox 3.x fonts
    private static final PDFont FONT_BODY  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_TITLE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public File exportToTxt(String title, String body, File destDir) throws Exception {
        if (!destDir.exists()) destDir.mkdirs();
        String safeTitle = safeName(title);
        File out = new File(destDir, safeTitle + "-" + TS.format(LocalDateTime.now()) + ".txt");
        try (FileWriter fw = new FileWriter(out)) {
            fw.write((title == null || title.isBlank()) ? "Untitled Note" : title);
            fw.write(System.lineSeparator());
            fw.write(System.lineSeparator());
            fw.write(body == null ? "" : body);
        }
        return out;
    }

    public File exportToPdf(String title, String body, File destDir) throws Exception {
        if (!destDir.exists()) destDir.mkdirs();
        String safeTitle = safeName(title);
        File out = new File(destDir, safeTitle + "-" + TS.format(LocalDateTime.now()) + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50f;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float usableWidth = pageWidth - margin * 2;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Title
            float y = pageHeight - margin;
            cs.beginText();
            cs.setFont(FONT_TITLE, TITLE_SIZE);
            cs.newLineAtOffset(margin, y);
            cs.showText((title == null || title.isBlank()) ? "Untitled Note" : title);
            cs.endText();

            y -= (TITLE_SIZE + 14f);

            // Body
            List<String> paragraphs = List.of((body == null ? "" : body).split("\\R\\R"));
            for (String para : paragraphs) {
                for (String line : wrap(para, usableWidth)) {
                    if (y < margin + LINE_HEIGHT) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = page.getMediaBox().getHeight() - margin;
                    }
                    cs.beginText();
                    cs.setFont(FONT_BODY, FONT_SIZE);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line);
                    cs.endText();
                    y -= LINE_HEIGHT;
                }
                y -= 10f;
            }

            cs.close();
            doc.save(out);
        }
        return out;
    }

    /** Copy a file into a chosen directory (e.g., Google Drive Notes). */
    public File exportFileToDriveFolder(File source, File driveFolder) throws Exception {
        if (!driveFolder.exists()) driveFolder.mkdirs();
        File dest = new File(driveFolder, source.getName());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    private static String safeName(String s) {
        if (s == null || s.isBlank()) return "note";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private static List<String> wrap(String text, float widthPx) throws Exception {
        var words = text.replace("\r", "").split("\\s+");
        var lines = new java.util.ArrayList<String>();
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String test = (line.length() == 0) ? w : line + " " + w;
            float testWidth = FONT_BODY.getStringWidth(test) / 1000f * FONT_SIZE;
            if (testWidth > widthPx && line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(w);
            } else {
                line.setLength(0);
                line.append(test);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    public interface FlashcardLike {
        String getFront();
        String getBack();
    }
}
