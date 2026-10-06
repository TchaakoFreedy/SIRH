package com.fric.sirh.service.pdf;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OcrSpaceService {

    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    @Value("${ocr.space.api.key:helloworld}")
    private String apiKey;

    @Value("${ocr.space.enabled:true}")
    private boolean enabled;

    // TIMEOUT AUGMENTÉS
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)   // 60 -> 120 secondes
            .readTimeout(120, TimeUnit.SECONDS)      // 60 -> 120 secondes
            .writeTimeout(120, TimeUnit.SECONDS)     // Ajouté pour l'envoi
            .callTimeout(120, TimeUnit.SECONDS)      // Timeout global
            .retryOnConnectionFailure(true)          // Réessayer en cas d'échec
            .build();

    private final Gson gson = new Gson();

    public String performOcr(BufferedImage image) {
        if (!enabled) {
            log.warn("⚠️ OCR.space est désactivé");
            return null;
        }

        // Max 2 tentatives
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 Envoi de l'image à OCR.space (tentative {}/{})...", attempt, maxRetries);

                // Redimensionner l'image pour accélérer
                BufferedImage resizedImage = image;
                if (image.getWidth() > 2000 || image.getHeight() > 2000) {
                    log.info("📐 Redimensionnement de l'image ({}x{} -> 2000x2000 max)",
                            image.getWidth(), image.getHeight());
                    resizedImage = resizeImage(image, 2000, 2000);
                }

                // Convertir en JPG (plus petit que PNG)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(resizedImage, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                log.info("📊 Taille de l'image: {} bytes", imageBytes.length);

                // Si > 1MB, réduire encore plus
                if (imageBytes.length > 1_000_000) {
                    log.warn("⚠️ Image trop grande ({} bytes), réduction supplémentaire...", imageBytes.length);
                    resizedImage = resizeImage(resizedImage, 1500, 1500);
                    baos.reset();
                    ImageIO.write(resizedImage, "jpg", baos);
                    imageBytes = baos.toByteArray();
                    log.info("📊 Taille après réduction: {} bytes", imageBytes.length);
                }

                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Paramètres optimisés
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("base64Image", "data:image/jpg;base64," + base64Image)
                        .addFormDataPart("language", "fre")
                        .addFormDataPart("apikey", apiKey)
                        .addFormDataPart("OCREngine", "1")      // Engine plus rapide
                        .addFormDataPart("scale", "false")       // Pas de scaling supplémentaire
                        .addFormDataPart("isTable", "false")     // Pas de détection de tableaux
                        .addFormDataPart("detectOrientation", "false")
                        .build();

                Request request = new Request.Builder()
                        .url(OCR_API_URL)
                        .post(body)
                        .build();

                long startTime = System.currentTimeMillis();

                try (Response response = client.newCall(request).execute()) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("⏱️ Durée de la requête: {}ms", duration);

                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        log.info("✅ OCR.space a répondu en {}ms", duration);

                        String text = extractTextFromJson(jsonResponse);

                        if (text != null && !text.trim().isEmpty()) {
                            log.info("✅ OCR.space a extrait {} caractères", text.length());
                            log.info("📄 Aperçu: {}...", text.substring(0, Math.min(200, text.length())));
                            return text;
                        } else {
                            log.warn("⚠️ OCR.space n'a pas extrait de texte");
                            // Si tentative < max, on réessaie
                            if (attempt < maxRetries) {
                                log.info("🔄 Nouvelle tentative dans 2 secondes...");
                                Thread.sleep(2000);
                                continue;
                            }
                            return null;
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No body";
                        log.error("❌ OCR.space a échoué avec le code: {}", response.code());
                        log.error("❌ Réponse d'erreur: {}", errorBody);

                        if (attempt < maxRetries) {
                            log.info("🔄 Nouvelle tentative dans 2 secondes...");
                            Thread.sleep(2000);
                            continue;
                        }
                        return null;
                    }
                }

            } catch (Exception e) {
                log.error("❌ Erreur OCR.space (tentative {}/{}): {}", attempt, maxRetries, e.getMessage());
                if (attempt < maxRetries) {
                    log.info("🔄 Nouvelle tentative dans 2 secondes...");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    return null;
                }
            }
        }

        return null;
    }

    private BufferedImage resizeImage(BufferedImage original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return original;
        }

        double ratio = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int newWidth = (int) (width * ratio);
        int newHeight = (int) (height * ratio);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private String extractTextFromJson(String json) {
        try {
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

            // Vérifier si l'OCR a réussi
            boolean isErroredOnProcessing = jsonObject.get("IsErroredOnProcessing").getAsBoolean();
            if (isErroredOnProcessing) {
                String errorMessage = "Erreur inconnue";
                if (jsonObject.get("ErrorMessage") != null) {
                    errorMessage = jsonObject.get("ErrorMessage").getAsString();
                }
                log.error("❌ OCR.space erreur de traitement: {}", errorMessage);
                return null;
            }

            // Extraire le texte
            var parsedResults = jsonObject.getAsJsonArray("ParsedResults");
            if (parsedResults != null && parsedResults.size() > 0) {
                var firstResult = parsedResults.get(0).getAsJsonObject();
                if (firstResult.get("ParsedText") != null) {
                    String text = firstResult.get("ParsedText").getAsString();
                    return text;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ Erreur parsing JSON OCR: {}", e.getMessage());
            return null;
        }
    }
}