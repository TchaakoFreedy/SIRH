package com.fric.sirh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/file/{filename}")
    public ResponseEntity<String> debugFile(@PathVariable String filename) {
        // Check multiple locations
        Path uploadsDir = Paths.get("uploads");
        Path uploadsContratsDir = Paths.get("uploads/contrats");
        Path fileInUploads = uploadsDir.resolve(filename);
        Path fileInContrats = uploadsContratsDir.resolve(filename);

        StringBuilder result = new StringBuilder();
        result.append("File: ").append(filename).append("\n");
        result.append("Uploads dir exists: ").append(Files.exists(uploadsDir)).append("\n");
        result.append("Uploads/contrats dir exists: ").append(Files.exists(uploadsContratsDir)).append("\n");
        result.append("File in uploads: ").append(fileInUploads.toAbsolutePath()).append(" - exists: ").append(Files.exists(fileInUploads)).append("\n");
        result.append("File in uploads/contrats: ").append(fileInContrats.toAbsolutePath()).append(" - exists: ").append(Files.exists(fileInContrats)).append("\n");

        // List files in directories
        try {
            if (Files.exists(uploadsDir)) {
                result.append("\nFiles in uploads:\n");
                Files.list(uploadsDir).forEach(p -> result.append("  - ").append(p.getFileName()).append("\n"));
            }
            if (Files.exists(uploadsContratsDir)) {
                result.append("\nFiles in uploads/contrats:\n");
                Files.list(uploadsContratsDir).forEach(p -> result.append("  - ").append(p.getFileName()).append("\n"));
            }

            // Also check the current working directory
            Path currentDir = Paths.get(".");
            result.append("\nCurrent working directory: ").append(currentDir.toAbsolutePath()).append("\n");

        } catch (IOException e) {
            result.append("Error listing files: ").append(e.getMessage());
        }

        return ResponseEntity.ok(result.toString());
    }
}