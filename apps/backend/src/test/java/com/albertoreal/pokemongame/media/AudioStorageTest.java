package com.albertoreal.pokemongame.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AudioStorageTest {

    @Test
    void writesFileToDatedDirectory(@TempDir Path tmp) throws Exception {
        var storage = new AudioStorage(new MediaConfig(tmp.toString()));
        var path = storage.store(LocalDate.of(2026, 4, 22), "q1.mp3", "hello".getBytes());

        assertThat(Files.exists(path)).isTrue();
        assertThat(Files.readString(path)).isEqualTo("hello");
        assertThat(storage.relativePath(LocalDate.of(2026, 4, 22), "q1.mp3"))
            .isEqualTo("/audio/2026-04-22/q1.mp3");
    }

    @Test
    void createsParentDirectoriesIfMissing(@TempDir Path tmp) throws Exception {
        var storage = new AudioStorage(new MediaConfig(tmp.resolve("nested").resolve("deep").toString()));
        var path = storage.store(LocalDate.of(2026, 5, 1), "q2.mp3", "x".getBytes());
        assertThat(Files.exists(path)).isTrue();
    }
}
