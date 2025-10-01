package com.cab302.eduplanner.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility component that normalises different document formats into plain text so they can be sent to LLMs.
 */
public class DocumentTextExtractor {

    private static final int MAX_CHARACTERS = 12_000;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Reads the provided file, extracts its textual contents and normalises whitespace.
     *
     * @param path the path to the file that should be converted to text
     * @return a trimmed and truncated text representation of the file
     * @throws IOException if the file cannot be read or the format is unsupported
     */
    public String extractText(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (!Files.exists(path)) {
            throw new IOException("File does not exist: " + path);
        }

        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".pdf")) {
            return prepareFromPdf(path);
        }
        if (fileName.endsWith(".docx")) {
            return prepareFromDocx(path);
        }
        if (fileName.endsWith(".txt")) {
            return prepareFromText(path);
        }

        throw new IOException("Unsupported file type: " + fileName);
    }

    private String prepareFromPdf(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             org.apache.pdfbox.pdmodel.PDDocument document = Loader.loadPDF((RandomAccessRead) inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return prepareText(stripper.getText(document));
        }
    }

    private String prepareFromDocx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return prepareText(extractor.getText());
        }
    }

    private String prepareFromText(Path path) throws IOException {
        return prepareText(Files.readString(path, StandardCharsets.UTF_8));
    }

    private String prepareText(String rawText) {
        if (rawText == null) {
            return "";
        }
        String normalised = WHITESPACE.matcher(rawText).replaceAll(" ").trim();
        if (normalised.length() > MAX_CHARACTERS) {
            return normalised.substring(0, MAX_CHARACTERS);
        }
        return normalised;
    }
}