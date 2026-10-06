package com.fric.sirh.controller;

import com.fric.sirh.dto.BulletinPaieDTO;
import com.fric.sirh.dto.BulletinPaieUploadResponse;
import com.fric.sirh.service.BulletinPaieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/pay-slips")
@RequiredArgsConstructor
public class BulletinPaieController {

    private final BulletinPaieService bulletinPaieService;

    private final ConcurrentHashMap<String, BulletinPaieUploadResponse> idempotencyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> activeUploads = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('PAYSLIP_CREATE')")
    public CompletableFuture<ResponseEntity<BulletinPaieUploadResponse>> uploadPaySlip(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Request-ID", required = false) String requestId,
            Authentication authentication) {

        cleanCache();

        String userId = authentication.getName();
        String sessionKey = userId + ":" + file.getOriginalFilename();

        log.info("Upload de bulletin de paie par l'utilisateur: {}, fichier: {}, requestId: {}",
                userId, file.getOriginalFilename(), requestId);

        if (idempotencyKey != null && idempotencyKey.trim().length() > 0) {
            BulletinPaieUploadResponse cached = idempotencyCache.get(idempotencyKey);
            if (cached != null) {
                log.info("Reponse en cache pour la cle {}: {} bulletins crees",
                        idempotencyKey, cached.getCreatedPayrolls());
                return CompletableFuture.completedFuture(ResponseEntity.ok(cached));
            }
        }

        String existingUpload = activeUploads.putIfAbsent(sessionKey, userId);
        if (existingUpload != null) {
            log.warn("Upload deja en cours pour {} avec le fichier {}",
                    userId, file.getOriginalFilename());
            BulletinPaieUploadResponse response = new BulletinPaieUploadResponse();
            response.setMessage("Upload deja en cours pour ce fichier");
            response.setStatus("DUPLICATE");
            return CompletableFuture.completedFuture(ResponseEntity.status(409).body(response));
        }

        try {
            return bulletinPaieService.processPaySlipUploadAsync(file, userId)
                    .thenApply(response -> {
                        if (idempotencyKey != null && idempotencyKey.trim().length() > 0) {
                            idempotencyCache.put(idempotencyKey, response);
                            cacheTimestamps.put(idempotencyKey, System.currentTimeMillis());
                        }
                        return ResponseEntity.ok(response);
                    })
                    .whenComplete((result, error) -> {
                        activeUploads.remove(sessionKey);
                        if (error != null) {
                            log.error("Erreur lors du traitement de l'upload: {}", error.getMessage());
                        }
                    });
        } catch (Exception e) {
            activeUploads.remove(sessionKey);
            log.error("Exception lors de l'upload: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW_ALL') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<List<BulletinPaieDTO>> getAllBulletins() {
        return ResponseEntity.ok(bulletinPaieService.getAllBulletins());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<BulletinPaieDTO> getBulletinById(@PathVariable String id) {
        return ResponseEntity.ok(bulletinPaieService.getBulletinById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYSLIP_UPDATE') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<BulletinPaieDTO> updateBulletin(
            @PathVariable String id,
            @RequestBody BulletinPaieDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(bulletinPaieService.updateBulletin(id, dto, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYSLIP_DELETE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBulletin(
            @PathVariable String id,
            Authentication authentication) {
        bulletinPaieService.deleteBulletin(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('PAYSLIP_DOWNLOAD') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadBulletin(@PathVariable String id) {
        Resource resource = bulletinPaieService.downloadBulletin(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW') or isAuthenticated()")
    public ResponseEntity<List<BulletinPaieDTO>> getMyBulletins(Authentication authentication) {
        return ResponseEntity.ok(bulletinPaieService.getBulletinsForCurrentUser(authentication));
    }

    @GetMapping("/{id}/pages")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getPages(@PathVariable String id) {
        return ResponseEntity.ok(bulletinPaieService.getPageIds(id));
    }

    @GetMapping("/{id}/download/page/{pageNumber}")
    @PreAuthorize("hasAuthority('PAYSLIP_DOWNLOAD') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadPage(@PathVariable String id, @PathVariable int pageNumber) {
        Resource resource = bulletinPaieService.getPageImage(id, pageNumber);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"page-" + pageNumber + ".png\"")
                .body(resource);
    }

    @GetMapping("/{id}/zip")
    @PreAuthorize("hasAuthority('PAYSLIP_DOWNLOAD') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadZip(@PathVariable String id) {
        Resource resource = bulletinPaieService.getBulletinZip(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bulletin-" + id + ".zip\"")
                .body(resource);
    }

    @GetMapping("/image/{imageId}")
    @PreAuthorize("hasAuthority('PAYSLIP_VIEW') or hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        Resource resource = bulletinPaieService.getImageResource(imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }

    private void cleanCache() {
        long now = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry -> now - entry.getValue() > CACHE_TTL_MS);
        idempotencyCache.keySet().removeIf(key -> !cacheTimestamps.containsKey(key));
    }
}