package com.albertoreal.pokemongame.media;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@RestController
@RequestMapping("/audio")
public class AudioController {

    private final MediaConfig config;

    public AudioController(MediaConfig config) {
        this.config = config;
    }

    @GetMapping("/{date}/{filename:.+}")
    public ResponseEntity<Resource> serve(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @PathVariable String filename) {
        // Basic path traversal protection
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path p = Path.of(config.audioDir()).resolve(date.toString()).resolve(filename);
        if (!Files.exists(p)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .body(new FileSystemResource(p));
    }
}
