// com.fric.sirh.service.pdf.PdfTextualDetectorService
package com.fric.sirh.service.pdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfTextualDetectorService {

    public boolean isTextualPdf(PDDocument document, int pageIndex) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);

        // On récupère le texte. S'il est vide ou trop court (< 20 caractères), c'est probablement une image.
        String text = stripper.getText(document);

        if (text == null || text.isBlank()) {
            log.warn("📄 Page {} - Aucun texte extrait, considéré comme scanné (image).", pageIndex);
            return false;
        }

        // On affiche les premiers caractères pour savoir ce que PDFBox a réellement lu.
        // C'est TRÈS important pour comprendre pourquoi il ne trouve pas les mots-clés.
        String preview = text.length() > 100 ? text.substring(0, 100) : text;
        log.info("📄 Page {} - Aperçu du texte PDFBox : '{}'...", pageIndex, preview.replace("\n", " "));

        String upper = text.toUpperCase();

        // On vérifie les mots-clés du bulletin AFRIC
        boolean isTextual = upper.contains("MATRICULE") ||
                upper.contains("NET A PAYER") ||
                upper.contains("SALAIRE DE BASE") ||
                upper.contains("BULLETIN DE PAIE");

        log.info("📄 Page {} - Détection textuelle : {}", pageIndex, isTextual);
        return isTextual;
    }
}