package com.fric.sirh.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
@Slf4j
public class PdfOcrService {

    @Autowired(required = false)
    private OcrSpaceService ocrSpaceService;

    @Autowired(required = false)
    private TesseractAutoService tesseractAutoService;

    @Value("${ocr.tesseract.enabled:false}")
    private boolean tesseractEnabled;

    @Value("${ocr.space.enabled:true}")
    private boolean ocrSpaceEnabled;

    public String performOcr(BufferedImage image) {
        log.info("🔍 Début de l'OCR...");
        log.info("📌 Tesseract activé: {}, OCR.space activé: {}", tesseractEnabled, ocrSpaceEnabled);

        String result = null;

        // 1. Essayer Tesseract SEULEMENT si activé
        if (tesseractEnabled && tesseractAutoService != null) {
            try {
                log.info("📌 Tentative OCR avec Tesseract...");
                result = tesseractAutoService.performOcr(image);
                if (result != null && !result.trim().isEmpty()) {
                    log.info("✅ OCR Tesseract réussi");
                    return result;
                }
            } catch (Exception e) {
                log.warn("⚠️ Tesseract a échoué: {}", e.getMessage());
            }
        } else {
            log.info("⏭️ Tesseract est désactivé, passage à OCR.space");
        }

        // 2. Essayer OCR.space SEULEMENT si activé
        if (ocrSpaceEnabled && ocrSpaceService != null) {
            try {
                log.info("📌 Tentative OCR avec OCR.space...");
                long startTime = System.currentTimeMillis();
                result = ocrSpaceService.performOcr(image);
                long duration = System.currentTimeMillis() - startTime;

                if (result != null && !result.trim().isEmpty()) {
                    log.info("✅ OCR.space réussi en {}ms, {} caractères extraits",
                            duration, result.length());
                    return result;
                }
            } catch (Exception e) {
                log.warn("⚠️ OCR.space a échoué: {}", e.getMessage());
            }
        }

        log.error("❌ Toutes les méthodes OCR ont échoué");
        return null;
    }
}