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
import java.util.ArrayList;
import java.util.List;

/** Export a flashcard deck to CSV/PDF, and copy into a target folder (e.g., Drive). */
public class FlashcardExportService {

    public static final class Card {
        public final String front;
        public final String back;
        public Card(String front, String back) {
            this.front = front == null ? "" : front;
            this.back  = back  == null ? "" : back;
        }
    }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    // PDFBox 3.x fonts
    private static final PDFont FONT_BODY  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /** Export as UTF-8 CSV: columns Front,Back with basic quoting. */
    public File exportCsv(String deckName, List<Card> cards, File destDir) throws Exception {
        if (!destDir.exists()) destDir.mkdirs();
        String base = safe(deckName.isBlank() ? "deck" : deckName);
        File out = new File(destDir, base + "-" + TS.format(LocalDateTime.now()) + ".csv");

        try (FileWriter fw = new FileWriter(out)) {
            fw.write("Front,Back\n");
            for (Card c : cards) {
                fw.write(csv(c.front));
                fw.write(",");
                fw.write(csv(c.back));
                fw.write("\n");
            }
        }
        return out;
    }

    /** Export a simple, readable PDF: title + Q/A blocks. */
    public File exportPdf(String deckName, List<Card> cards, File destDir) throws Exception {
        if (!destDir.exists()) destDir.mkdirs();
        String base = safe(deckName.isBlank() ? "deck" : deckName);
        File out = new File(destDir, base + "-" + TS.format(LocalDateTime.now()) + ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50f;
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float width = pageW - margin * 2;
            float y = pageH - margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Title
            cs.beginText();
            cs.setFont(FONT_BOLD, 18);
            cs.newLineAtOffset(margin, y);
            cs.showText("Deck: " + (deckName == null || deckName.isBlank() ? "Untitled" : deckName));
            cs.endText();
            y -= 26;

            for (Card card : cards) {
                // Ensure space
                if (y < margin + 80) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = page.getMediaBox().getHeight() - margin;
                }

                // Q:
                y = block(cs, "Q:", card.front, margin, y, width);

                // A:
                y = block(cs, "A:", card.back, margin, y, width);

                y -= 10;
            }

            cs.close();
            doc.save(out);
        }
        return out;
    }

    /** Copy a file into target folder (e.g., Drive/Flashcards). */
    public File copyToFolder(File source, File destFolder) throws Exception {
        if (!destFolder.exists()) destFolder.mkdirs();
        File dest = new File(destFolder, source.getName());
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    // ---------- helpers ----------

    private static float block(PDPageContentStream cs, String label, String text,
                               float x, float y, float width) throws Exception {
        cs.beginText();
        cs.setFont(FONT_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(label);
        cs.endText();
        y -= 16;

        for (String line : wrap(text, width, 11f)) {
            cs.beginText();
            cs.setFont(FONT_BODY, 11);
            cs.newLineAtOffset(x, y);
            cs.showText(line);
            cs.endText();
            y -= 14;
        }
        y -= 8;
        return y;
    }

    private static List<String> wrap(String text, float widthPx, float fontSize) throws Exception {
        var words = (text == null ? "" : text.replace("\r", "")).split("\\s+");
        var lines = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String test = (line.length() == 0) ? w : line + " " + w;
            float testWidth = FONT_BODY.getStringWidth(test) / 1000f * fontSize;
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

    private static String safe(String s) { return s.replaceAll("[^a-zA-Z0-9._-]+", "_"); }

    private static String csv(String s) {
        String v = s == null ? "" : s;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

