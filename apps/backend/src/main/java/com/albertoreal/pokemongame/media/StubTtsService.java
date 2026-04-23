package com.albertoreal.pokemongame.media;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!tts-real")
public class StubTtsService implements TextToSpeechService {

    @Override
    public byte[] synthesize(String text) {
        // Plan 1 placeholder: no real MP3 generation. Plan 2 will implement
        // Google Cloud TTS (neural es-ES-Chirp3-HD-*) and replace this stub.
        return ("STUB_TTS:" + (text == null ? "" : text)).getBytes();
    }
}
