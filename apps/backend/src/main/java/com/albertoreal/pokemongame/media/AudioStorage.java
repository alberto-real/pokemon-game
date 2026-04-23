package com.albertoreal.pokemongame.media;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

@Component
public class AudioStorage {

    private final Path baseDir;

    public AudioStorage(MediaConfig config) {
        this.baseDir = Path.of(config.audioDir());
    }

    public Path store(LocalDate date, String filename, byte[] content) {
        Path dir = baseDir.resolve(date.toString());
        Path file = dir.resolve(filename);
        try {
            Files.createDirectories(dir);
            Files.write(file, content);
        } catch (IOException e) {
            throw new UncheckedAudioException("Failed to write audio file: " + file, e);
        }
        return file;
    }

    public String relativePath(LocalDate date, String filename) {
        return "/audio/" + date + "/" + filename;
    }

    public static class UncheckedAudioException extends RuntimeException {
        public UncheckedAudioException(String msg, Throwable t) { super(msg, t); }
    }
}
