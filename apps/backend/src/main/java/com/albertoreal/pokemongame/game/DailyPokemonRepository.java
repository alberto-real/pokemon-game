package com.albertoreal.pokemongame.game;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPokemonRepository extends JpaRepository<DailyPokemon, LocalDate> {
    List<DailyPokemon> findAllByOrderByDateDesc();
    Optional<DailyPokemon> findByDate(LocalDate date);
}
