package com.albertoreal.pokemongame.media;

public interface TextToSpeechService {
    /** Returns the synthesized audio bytes (MP3 in prod; placeholder in Plan 1 stub). */
    byte[] synthesize(String text);
}
