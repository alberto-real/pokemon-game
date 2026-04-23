package com.albertoreal.pokemongame.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final String stubUser;

    public CurrentUser(@Value("${pokemon-game.auth.stub-user:dev}") String stubUser) {
        this.stubUser = stubUser;
    }

    public String userId() {
        return stubUser;
    }
}
