package com.albertoreal.pokemongame;

import org.springframework.boot.SpringApplication;

public class TestPokemonGameBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(PokemonGameBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
