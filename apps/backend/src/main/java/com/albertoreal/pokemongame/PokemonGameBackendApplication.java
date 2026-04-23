package com.albertoreal.pokemongame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PokemonGameBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PokemonGameBackendApplication.class, args);
	}

}
