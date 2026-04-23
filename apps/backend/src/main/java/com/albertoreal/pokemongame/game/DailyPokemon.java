package com.albertoreal.pokemongame.game;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "daily_pokemon")
public class DailyPokemon {

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "pokemon_id", nullable = false)
    private int pokemonId;

    @Column(name = "pokemon_name", nullable = false)
    private String pokemonName;

    @Column(name = "pokemon_name_es", nullable = false)
    private String pokemonNameEs;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    protected DailyPokemon() {}

    public DailyPokemon(LocalDate date, int pokemonId, String pokemonName,
                        String pokemonNameEs, String imageUrl, OffsetDateTime generatedAt) {
        this.date = date;
        this.pokemonId = pokemonId;
        this.pokemonName = pokemonName;
        this.pokemonNameEs = pokemonNameEs;
        this.imageUrl = imageUrl;
        this.generatedAt = generatedAt;
    }

    public LocalDate getDate() { return date; }
    public int getPokemonId() { return pokemonId; }
    public String getPokemonName() { return pokemonName; }
    public String getPokemonNameEs() { return pokemonNameEs; }
    public String getImageUrl() { return imageUrl; }
    public OffsetDateTime getGeneratedAt() { return generatedAt; }
}
