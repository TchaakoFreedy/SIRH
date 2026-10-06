package com.fric.sirh.service.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class PdfImageService {

    /**
     * Rend une page PDF en image avec la DPI spécifiée
     */
    public BufferedImage renderPageToImage(PDDocument document, int pageIndex, int dpi) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);

        // Utiliser RGB pour éviter les problèmes de couleur
        return renderer.renderImageWithDPI(pageIndex - 1, dpi, ImageType.RGB);
    }

    /**
     * Rend une page avec des paramètres optimisés pour l'extraction
     */
    public BufferedImage renderPageForExtraction(PDDocument document, int pageIndex) throws IOException {
        // DPI plus élevée pour une meilleure qualité
        BufferedImage image = renderPageToImage(document, pageIndex, 300);

        // Convertir en niveaux de gris pour améliorer le contraste
        BufferedImage grayImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        java.awt.Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return grayImage;
    }

    /**
     * Convertit une image en tableau de bytes (PNG)
     */
    public byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}