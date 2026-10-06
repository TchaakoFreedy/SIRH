package com.fric.sirh.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RegexExtractor {

    public Map<String, String> extract(String text) {
        Map<String, String> data = new HashMap<>();

        if (text == null || text.isEmpty() ||
                text.equals("AUCUN_TEXTE_DETECTE") ||
                text.equals("PDF_SANS_TEXTE")) {
            log.warn("⚠️ Aucun texte valide à analyser");
            return data;
        }

        // Nettoyer le texte - important pour l'OCR
        String cleanText = text
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\|", " ")
                .replaceAll("_", " ")
                .trim();

        log.debug("Texte nettoyé (premiers 500 caractères): {}",
                cleanText.substring(0, Math.min(500, cleanText.length())));

        // ===== MATRICULE =====
        String matricule = extractMatricule(cleanText);
        if (matricule != null && !matricule.isEmpty() &&
                !matricule.equalsIgnoreCase("Nom") &&
                !matricule.equalsIgnoreCase("null")) {
            data.put("matricule", matricule);
            log.info("✅ Matricule extrait: {}", matricule);
        } else {
            log.warn("⚠️ Aucun matricule extrait");
        }

        // ===== NOM =====
        String nom = extractNom(cleanText);
        if (nom != null && !nom.isEmpty() &&
                !nom.equalsIgnoreCase("null") &&
                !nom.equalsIgnoreCase("Nom") &&
                nom.length() > 2) {
            data.put("nom", nom);
            log.info("✅ Nom extrait: {}", nom);
        }

        // ===== SALAIRE BRUT =====
        String brut = extractBrut(cleanText);
        if (brut != null && !brut.isEmpty() && !brut.equalsIgnoreCase("null")) {
            data.put("brut", cleanNumber(brut));
            data.put("salaireBase", cleanNumber(brut));
            log.info("✅ Brut extrait: {}", brut);
        }

        // ===== SALAIRE NET =====
        String net = extractNet(cleanText);
        if (net != null && !net.isEmpty() && !net.equalsIgnoreCase("null")) {
            data.put("net", cleanNumber(net));
            data.put("netAPayer", cleanNumber(net));
            log.info("✅ Net extrait: {}", net);
        } else {
            // Essayer de trouver le net dans le tableau
            String netTableau = extractNetFromTableau(cleanText);
            if (netTableau != null) {
                data.put("net", cleanNumber(netTableau));
                data.put("netAPayer", cleanNumber(netTableau));
                log.info("✅ Net extrait du tableau: {}", netTableau);
            }
        }

        // ===== DÉDUCTIONS =====
        String deductions = extractDeductions(cleanText);
        if (deductions != null && !deductions.isEmpty() && !deductions.equalsIgnoreCase("null")) {
            data.put("deductions", cleanNumber(deductions));
            log.info("✅ Déductions extraites: {}", deductions);
        }

        // ===== PÉRIODE =====
        String periode = extractPeriode(cleanText);
        if (periode != null && !periode.isEmpty() && !periode.equalsIgnoreCase("null")) {
            data.put("periode", periode);
            log.info("✅ Période extraite: {}", periode);
        }

        // ===== DATE D'EMBAUCHE =====
        String embauche = extractEmbauche(cleanText);
        if (embauche != null && !embauche.isEmpty()) {
            data.put("embauche", embauche);
            log.info("✅ Date d'embauche extraite: {}", embauche);
        }

        log.info("📊 Données extraites finales: {}", data);
        return data;
    }

    private String extractMatricule(String text) {
        // Chercher le matricule format "M031912756667H"
        Pattern specialPattern = Pattern.compile(
                "M\\d{12}[A-Z]",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = specialPattern.matcher(text);
        if (m.find()) {
            return m.group();
        }

        // Chercher "Matricule: XXX"
        Pattern matcherPattern = Pattern.compile(
                "Matricule\\s*[:.]?\\s*([A-Z]{2,3}[-]?[0-9]{3,6})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = matcherPattern.matcher(text);
        if (m2.find()) {
            String value = m2.group(1).trim();
            if (!value.toUpperCase().contains("NPS") &&
                    !value.equalsIgnoreCase("Nom") &&
                    value.length() >= 3) {
                return value;
            }
        }

        // Chercher tout pattern qui ressemble à un matricule
        Pattern pattern = Pattern.compile(
                "([A-Z]{2,3}[-]?[0-9]{3,6})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m3 = pattern.matcher(text);
        while (m3.find()) {
            String value = m3.group(1).trim();
            if (!value.toUpperCase().contains("NPS") &&
                    !value.equalsIgnoreCase("Nom") &&
                    !value.equalsIgnoreCase("null")) {
                return value;
            }
        }

        return null;
    }

    private String extractNom(String text) {
        // Chercher "Mme XXXX" ou "Mlle XXXX"
        Pattern pattern = Pattern.compile(
                "(?:M(?:lle|me|r)\\s+([A-Za-z\\s\\-]{2,30}))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String value = m.group(1).trim();
            if (!value.isEmpty() && !value.equalsIgnoreCase("null") && value.length() > 2) {
                return value;
            }
        }

        // Chercher "Nom: XXX"
        Pattern pattern2 = Pattern.compile(
                "(?:Nom|NOM)\\s*[:.]?\\s*([A-Za-z\\s\\-]{2,30})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m2 = pattern2.matcher(text);
        if (m2.find()) {
            String value = m2.group(1).trim();
            if (!value.isEmpty() && !value.equalsIgnoreCase("null") && value.length() > 2) {
                return value;
            }
        }

        return null;
    }

    private String extractBrut(String text) {
        // Chercher "SALAIRE DE BASE" avec le montant
        Pattern pattern = Pattern.compile(
                "(?:SALAIRE DE BASE|Salaire de base)\\s*(?:\\d{4}\\s+)?(\\d+[\\.\\,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        // Chercher dans le tableau
        Pattern tableauPattern = Pattern.compile(
                "0001\\s*\\|?\\s*SALAIRE DE BASE\\s*\\|?\\s*(\\d+[\\.,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher tableauM = tableauPattern.matcher(text);
        if (tableauM.find()) {
            return tableauM.group(1).trim();
        }

        return null;
    }

    private String extractNet(String text) {
        // Chercher "NET A PAYER" avec le montant
        Pattern pattern = Pattern.compile(
                "(?:NET A PAYER|NET À PAYER)\\s*[:.]?\\s*(\\d+[\\.\\,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractNetFromTableau(String text) {
        // Chercher dans le tableau en bas
        Pattern pattern = Pattern.compile(
                "NET A PAYER\\s*\\|?\\s*(\\d+[\\.\\,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractDeductions(String text) {
        // Chercher "Total retenues"
        Pattern pattern = Pattern.compile(
                "Total\\s*retenues\\s*(?:obligatoires)?\\s*[:.]?\\s*(\\d+[\\.\\,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private String extractPeriode(String text) {
        Pattern pattern = Pattern.compile(
                "P[ée]riode\\s*[:.]?\\s*([A-Za-z]{3,9}\\s+\\d{4})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }

        Pattern pattern2 = Pattern.compile(
                "P[ée]riode\\s*[:.]?\\s*(\\d{2}/\\d{4})",
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
                "Embauche\\s*[:.]?\\s*(\\d{2}/\\d{2}/\\d{4})",
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