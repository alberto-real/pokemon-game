package com.albertoreal.pokemongame.common;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void returnsConfiguredStubUser() {
        var user = new CurrentUser("dev");
        assertThat(user.userId()).isEqualTo("dev");
    }

    @Test
    void respectsCustomValue() {
        var user = new CurrentUser("alberto");
        assertThat(user.userId()).isEqualTo("alberto");
    }
}
