package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PickupCodesTest {

    @Test
    void generatesSixReadableCharactersWithoutAmbiguousOnes() {
        for (int i = 0; i < 500; i++) {
            assertThat(PickupCodes.generate()).matches("[ABCDEFGHJKMNPQRSTUVWXYZ2-9]{6}");
        }
    }

    @Test
    void normalizeIgnoresCaseSpacesAndDashes() {
        assertThat(PickupCodes.normalize(" k7m-2px ")).isEqualTo("K7M2PX");
        assertThat(PickupCodes.normalize(null)).isEmpty();
    }
}
