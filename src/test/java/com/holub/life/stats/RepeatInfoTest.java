package com.holub.life.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RepeatInfoTest {

    @Test
    void periodMustBeAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> new RepeatInfo(0));
    }
}
