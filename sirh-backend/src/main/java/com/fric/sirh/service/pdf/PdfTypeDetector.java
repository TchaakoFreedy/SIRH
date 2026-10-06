// com.fric.sirh.service.pdf.PdfTypeDetector (Modifié)
package com.fric.sirh.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfTypeDetector {

    public boolean isTextPage(PDDocument document, int pageIndex) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        String text = stripper.getText(document);
        if (text == null) return false;
        String clean = text.replaceAll("\\s+", "").trim();
        return clean.length() > 10;
    }

    // Nouvelle méthode pour vérifier si c'est le bulletin Fric/Afric
    public boolean isAfricBulletin(PDDocument document, int pageIndex) throws Exception {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(pageIndex);
        stripper.setEndPage(pageIndex);
        String text = stripper.getText(document);

        if (text == null) return false;

        // On vérifie la présence des marqueurs clés du bulletin
        String upperText = text.toUpperCase();
        return upperText.contains("AFRIC PAYMENT SOLUTION") ||
                (upperText.contains("BULLETIN DE PAIE") && upperText.contains("MATRICULE"));
    }
}