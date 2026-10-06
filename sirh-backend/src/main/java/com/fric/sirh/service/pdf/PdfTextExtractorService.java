package com.fric.sirh.service.pdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfTextExtractorService {

    private final PdfImageService pdfImageService;
    private final PdfOcrService pdfOcrService;

    /**
     * Extrait le texte d'une page avec fallback OCR automatique
     * Cette méthode est appelée par BulletinPaieService
     */
    public String extractLinesFromPage(PDDocument document, int pageIndex) throws IOException {
        log.info("📄 Page {} - Début de l'extraction du texte...", pageIndex);

        // 1. D'abord, essayer d'extraire le texte avec PDFBox
        String pdfBoxText = extractPdfBoxText(document, pageIndex);
        log.info("📄 Page {} - PDFBox a extrait {} caractères",
                pageIndex, pdfBoxText != null ? pdfBoxText.length() : 0);

        // 2. Vérifier si le texte PDFBox contient les mots-clés du bulletin AFRIC
        if (pdfBoxText != null && !pdfBoxText.trim().isEmpty()) {
            String upperText = pdfBoxText.toUpperCase();

            // Mots-clés spécifiques au bulletin AFRIC
            boolean hasAfricKeywords = upperText.contains("AFRIC") ||
                    upperText.contains("BULLETIN DE PAIE") ||
                    upperText.contains("SALAIRE DE BASE") ||
                    upperText.contains("NET A PAYER") ||
                    upperText.contains("MATRICULE") ||
                    upperText.matches(".*M\\d{12}[A-Z].*"); // Matricule format spécial

            if (hasAfricKeywords) {
                log.info("✅ Page {} - PDFBox a trouvé les mots-clés du bulletin AFRIC", pageIndex);
                return pdfBoxText;
            }
        }

        // 3. Si PDFBox n'a pas trouvé assez de texte, utiliser OCR
        log.info("📄 Page {} - Texte PDFBox insuffisant, tentative OCR...", pageIndex);

        try {
            // Rendre la page en image pour OCR (300 DPI pour meilleure qualité)
            BufferedImage image = pdfImageService.renderPageForExtraction(document, pageIndex);
            log.info("📄 Page {} - Image générée: {}x{} pixels",
                    pageIndex, image.getWidth(), image.getHeight());

            // Utiliser le service OCR (Tesseract ou OCR.space)
            String ocrText = pdfOcrService.performOcr(image);

            if (ocrText != null && !ocrText.trim().isEmpty()) {
                log.info("✅ Page {} - OCR a extrait {} caractères", pageIndex, ocrText.length());

                // Nettoyer le texte OCR
                String cleanOcrText = ocrText
                        .replaceAll("\\s+", " ")
                        .replaceAll("\\|", " ")
                        .trim();

                // Vérifier que l'OCR a trouvé des données pertinentes
                if (cleanOcrText.length() > 100) {
                    log.info("📄 Page {} - Aperçu OCR: {}...",
                            pageIndex,
                            cleanOcrText.substring(0, Math.min(300, cleanOcrText.length())));
                    return cleanOcrText;
                } else if (cleanOcrText.length() > 50) {
                    log.warn("⚠️ Page {} - OCR a extrait peu de texte ({} caractères)",
                            pageIndex, cleanOcrText.length());
                    return cleanOcrText;
                }
            } else {
                log.warn("⚠️ Page {} - OCR n'a retourné aucun texte", pageIndex);
            }

        } catch (Exception e) {
            log.error("❌ Page {} - Erreur OCR: {}", pageIndex, e.getMessage(), e);
        }

        // 4. Fallback: retourner ce qu'on a trouvé avec PDFBox même si incomplet
        if (pdfBoxText != null && !pdfBoxText.trim().isEmpty()) {
            log.warn("⚠️ Page {} - Retour du texte PDFBox incomplet ({} caractères)",
                    pageIndex, pdfBoxText.length());
            return pdfBoxText;
        }

        return "AUCUN_TEXTE_DETECTE";
    }

    /**
     * Extrait le texte avec PDFBox standard
     */
    private String extractPdfBoxText(PDDocument document, int pageIndex) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        stripper.setSortByPosition(true);
        stripper.setWordSeparator(" ");
        stripper.setLineSeparator("\n");

        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        String text = writer.toString();
        if (text != null) {
            text = text.replaceAll("\\s+", " ").trim();
        }

        return text;
    }

    /**
     * Extrait le texte standard d'une page (méthode existante)
     */
    public String extractTextFromPage(PDDocument document, int pageIndex) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        stripper.setSortByPosition(true);

        String text = stripper.getText(document);
        return text != null ? text.trim() : null;
    }

    /**
     * Extrait le texte avec préservation des colonnes (méthode existante)
     */
    public String extractTextWithColumns(PDDocument document, int pageIndex, float minColumnWidth) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper() {
            private final List<String> lines = new ArrayList<>();
            private float pageWidth = 0;

            @Override
            public void startPage(PDPage page) throws IOException {
                PDRectangle mediaBox = page.getMediaBox();
                pageWidth = mediaBox.getWidth();
                super.startPage(page);
            }

            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                if (textPositions == null || textPositions.isEmpty()) return;

                float avgX = textPositions.stream()
                        .map(TextPosition::getX)
                        .reduce(0f, Float::sum) / textPositions.size();

                int column = (int) (avgX / minColumnWidth);
                String textLine = text.trim();

                while (lines.size() <= column) {
                    lines.add("");
                }

                String current = lines.get(column);
                if (!current.isEmpty()) {
                    lines.set(column, current + " " + textLine);
                } else {
                    lines.set(column, textLine);
                }
            }

            @Override
            public void endPage(PDPage page) throws IOException {
                StringBuilder result = new StringBuilder();
                int maxLines = lines.stream().mapToInt(l -> l.split("\n").length).max().orElse(0);

                for (int i = 0; i < maxLines; i++) {
                    for (String columnText : lines) {
                        String[] parts = columnText.split("\n");
                        if (i < parts.length) {
                            result.append(parts[i]).append("\t");
                        } else {
                            result.append("\t");
                        }
                    }
                    result.append("\n");
                }

                super.writeString(result.toString(), null);
                lines.clear();
            }
        };

        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        stripper.setSortByPosition(true);

        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);
        return writer.toString();
    }

    /**
     * Vérifie si une page contient du texte (méthode existante)
     */
    public boolean hasText(PDDocument document, int pageIndex) throws IOException {
        String text = extractTextFromPage(document, pageIndex);
        return text != null && !text.trim().isEmpty() && text.trim().length() > 10;
    }

    /**
     * Détecte les mots-clés importants (méthode existante)
     */
    public boolean containsKeywords(String text, String... keywords) {
        if (text == null || text.isEmpty()) return false;

        String upperText = text.toUpperCase();
        for (String keyword : keywords) {
            if (upperText.contains(keyword.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}