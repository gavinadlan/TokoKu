package com.tokoku.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    void variantsInheritBaseAndOverrideAccent() {
        for (String n : new String[] {"base", "a", "b", "c"}) {
            Theme t = Theme.of(n);
            for (String k : new String[] {"paper", "ink", "accent", "on-accent", "card", "invoice-accent", "invoice-line"}) {
                assertTrue(t.get(k).matches("#[0-9A-Fa-f]{6}"), n + "." + k);
            }
        }
        assertEquals(Theme.of("base").get("ink"), Theme.of("a").get("ink"));
        assertNotEquals(Theme.of("base").get("accent"), Theme.of("a").get("accent"));
        assertTrue(Theme.of("c").sansDisplay());
        assertThrows(IllegalArgumentException.class, () -> Theme.of("zzz"));
    }
}
