package com.fric.sirh.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AfricBulletinParserService {

    public Map<String, String> parseBulletinData(String text) {
        Map<String, String> extractedData = new HashMap<>();
        if (text == null || text.isEmpty() || text.equals("AUCUN_TEXTE_DETECTE")) {
            log.warn("⚠️ Texte vide ou non détecté");
            return extractedData;
        }

        // Nettoyer le texte
        String cleanText = text
                .replaceAll("\\s+", " ")
                .replaceAll("\\|", " ")
                .replaceAll("_", " ")
                .trim();

        log.debug("📄 Texte nettoyé (premiers 500 caractères): {}",
                cleanText.substring(0, Math.min(500, cleanText.length())));

        // 1. Extraction du Matricule - chercher B00028 en priorité
        String matricule = extractMatricule(cleanText);
        if (matricule != null && !matricule.isEmpty()) {
            extractedData.put("matricule", matricule);
            log.info("✅ Matricule extrait: {}", matricule);
        } else {
            log.warn("⚠️ Aucun matricule trouvé");
        }

        // 2. Extraction du Nom (Mlle + nom)
        String nom = extractNom(cleanText);
        if (nom != null && !nom.isEmpty()) {
            extractedData.put("nom", nom);
            log.info("✅ Nom extrait: {}", nom);
        }

        // 3. Extraction du Salaire de Base
        String salaireBase = extractSalaireBase(cleanText);
        if (salaireBase != null && !salaireBase.isEmpty()) {
            extractedData.put("brut", salaireBase);
            extractedData.put("salaireBase", salaireBase);
            log.info("✅ Salaire de base extrait: {}", salaireBase);
        }

        // 4. Extraction du NET A PAYER
        String netAPayer = extractNetAPayer(cleanText);
        if (netAPayer != null && !netAPayer.isEmpty()) {
            extractedData.put("netAPayer", netAPayer);
            extractedData.put("net", netAPayer);
            log.info("✅ Net à payer extrait: {}", netAPayer);
        } else {
            log.warn("⚠️ Aucun Net à payer trouvé");
        }

        // 5. Extraction du Total des retenues
        String totalRetenues = extractTotalRetenues(cleanText);
        if (totalRetenues != null && !totalRetenues.isEmpty()) {
            extractedData.put("deductions", totalRetenues);
            log.info("✅ Total retenues extrait: {}", totalRetenues);
        }

        // 6. Extraction de la période
        String periode = extractPeriode(cleanText);
        if (periode != null && !periode.isEmpty()) {
            extractedData.put("periode", periode);
            log.info("✅ Période extraite: {}", periode);
        }

        // 7. Extraction de la date d'embauche
        String embauche = extractEmbauche(cleanText);
        if (embauche != null && !embauche.isEmpty()) {
            extractedData.put("embauche", embauche);
            log.info("✅ Date d'embauche extraite: {}", embauche);
        }

        // 8. Extraction du NIU (M031912756667H) pour information
        String niu = extractNIU(cleanText);
        if (niu != null && !niu.isEmpty()) {
            extractedData.put("niu", niu);
            log.info("✅ NIU extrait: {}", niu);
        }

        log.info("📊 Données extraites: {}", extractedData);
        return extractedData;
    }

    private String extractMatricule(String text) {
        // 1. Chercher "B00028" (format exact du matricule)
        Pattern exactPattern = Pattern.compile(
                "B\\d{5}",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = exactPattern.matcher(text);
        if (m.find()) {
            String value = m.group().trim();
            log.info("🔍 Matricule trouvé (format BXXXXX): {}", value);
            return value;
        }

        // 2. Chercher "Matricule B00028" ou "Matricule: B00028"
        Pattern matriculePattern = Pattern.compile(
                "Matricule\\s*[:.]?\\s*([A-Z]\\d{4,6})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = matriculePattern.matcher(text);
        if (m2.find()) {
            String value = m2.group(1).trim();
            if (!value.equalsIgnoreCase("EMPLOYE") &&
                    !value.equalsIgnoreCase("null") &&
                    value.length() >= 5) {
                log.info("🔍 Matricule trouvé (format Matricule XXX): {}", value);
                return value;
            }
        }

        // 3. Chercher "B" suivi de 5 chiffres dans le texte
        Pattern fallbackPattern = Pattern.compile(
                "\\b(B\\d{5})\\b"
        );
        Matcher m3 = fallbackPattern.matcher(text);
        if (m3.find()) {
            String value = m3.group(1).trim();
            log.info("🔍 Matricule trouvé (fallback): {}", value);
            return value;
        }

        return null;
    }

    private String extractNom(String text) {
        // Chercher "Mlle XXXX" ou "Mme XXXX" ou "Mr XXXX"
        Pattern pattern = Pattern.compile(
                "(M(?:lle|me|r)\\s+([A-Za-z\\s\\-]{2,30}))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String value = m.group(1).trim();
            if (!value.isEmpty() && !value.equalsIgnoreCase("null") && value.length() > 2) {
                // Vérifier que ce n'est pas un en-tête de tableau
                if (!value.contains("Base") && !value.contains("Taux") && !value.contains("Brut")) {
                    return value;
                }
            }
        }
        return null;
    }

    private String extractSalaireBase(String text) {
        // Chercher "Salaire de Base 381051.06" ou similaire
        Pattern pattern = Pattern.compile(
                "Salaire de Base\\s+([0-9]+[\\.,]?[0-9]*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return cleanNumber(m.group(1));
        }
        return null;
    }

    private String extractNetAPayer(String text) {
        // Chercher "NET A PAYER" avec le montant
        Pattern pattern = Pattern.compile(
                "NET\\s*A\\s*PAYER\\s*(\\d+[\\.\\,]?\\d+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String value = m.group(1);
            if (value.length() > 3) {
                return cleanNumber(value);
            }
        }

        // Chercher "Net à payer" avec le montant
        Pattern pattern2 = Pattern.compile(
                "Net\\s*[àa]\\s*payer\\s*(\\d+[\\.\\,]?\\d+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = pattern2.matcher(text);
        if (m2.find()) {
            String value = m2.group(1);
            if (value.length() > 3) {
                return cleanNumber(value);
            }
        }

        return null;
    }

    private String extractTotalRetenues(String text) {
        Pattern pattern = Pattern.compile(
                "Total\\s*retenues\\s*(?:obligatoires)?\\s*(\\d+[\\.\\,]?\\d+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String value = m.group(1);
            if (value.length() > 3) {
                return cleanNumber(value);
            }
        }
        return null;
    }

    private String extractPeriode(String text) {
        Pattern pattern = Pattern.compile(
                "Période\\s*du\\s*[:.]?\\s*(\\d{2}/\\d{2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        Pattern pattern2 = Pattern.compile(
                "Période\\s*[:.]?\\s*(\\d{2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = pattern2.matcher(text);
        if (m2.find()) {
            return m2.group(1).trim();
        }

        return null;
    }

    private String extractEmbauche(String text) {
        Pattern pattern = Pattern.compile(
                "Embauche\\s*(\\d{2}/\\d{2}/\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractNIU(String text) {
        // Chercher le format M031912756667H
        Pattern pattern = Pattern.compile(
                "(M\\d{12}[A-Z])",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String cleanNumber(String value) {
        if (value == null || value.isEmpty()) return null;
        String cleaned = value.replaceAll("\\s", "");
        cleaned = cleaned.replace(",", ".");
        if (!cleaned.contains(".")) {
            cleaned = cleaned + ".0";
        }
        return cleaned;
    }
}