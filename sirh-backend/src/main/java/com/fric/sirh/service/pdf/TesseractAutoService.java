package com.fric.sirh.service.pdf;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
@Slf4j
public class TesseractAutoService {

    @Autowired(required = false)
    private Tesseract tesseract;

    @Value("${ocr.tesseract.enabled:false}")
    private boolean enabled;  // Default to false!

    public String performOcr(BufferedImage image) {
        // CHECK THIS FIRST - If disabled, return immediately
        if (!enabled) {
            log.info("⏭️ Tesseract est désactivé (ocr.tesseract.enabled=false)");
            return null;
        }

        if (tesseract == null) {
            log.error("❌ Tesseract n'est pas disponible");
            return null;
        }

        try {
            log.info("🔍 Exécution OCR avec Tesseract...");
            long startTime = System.currentTimeMillis();

            String result = tesseract.doOCR(image);
            long duration = System.currentTimeMillis() - startTime;

            if (result == null || result.trim().isEmpty()) {
                log.warn("⚠️ Tesseract n'a pas détecté de texte (durée: {}ms)", duration);
                return null;
            }

            String cleanedResult = result
                    .replaceAll("\\s+", " ")
                    .trim();

            log.info("✅ Tesseract réussi en {}ms: {} caractères extraits",
                    duration, cleanedResult.length());

            return cleanedResult;

        } catch (TesseractException e) {
            log.error("❌ Erreur Tesseract: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Erreur inattendue: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return enabled && tesseract != null;
    }
}