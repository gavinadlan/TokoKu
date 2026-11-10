package com.tokoku.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tokoku.store.StoreService;
import com.tokoku.store.StoreSettings;

class ShippingCalculatorTest {

    private ShippingCalculator calculator(String fee, String threshold) {
        StoreSettings s = mock(StoreSettings.class);
        when(s.getFlatShippingFee()).thenReturn(new BigDecimal(fee));
        when(s.getFreeShippingThreshold()).thenReturn(threshold == null ? null : new BigDecimal(threshold));
        StoreService store = mock(StoreService.class);
        when(store.current()).thenReturn(s);
        return new FlatRateShippingCalculator(store);
    }

    private static BigDecimal d(String v) { return new BigDecimal(v); }

    @Test
    void flatFeeBelowThreshold() {
        assertThat(calculator("15000", "500000").fee(FulfillmentType.DELIVERY, d("377000"))).isEqualByComparingTo("15000");
    }

    @Test
    void freeAtOrAboveThreshold() {
        var c = calculator("15000", "500000");
        assertThat(c.fee(FulfillmentType.DELIVERY, d("500000"))).isEqualByComparingTo("0");
        assertThat(c.fee(FulfillmentType.DELIVERY, d("820000"))).isEqualByComparingTo("0");
    }

    @Test
    void noThresholdMeansAlwaysFlat() {
        assertThat(calculator("15000", null).fee(FulfillmentType.DELIVERY, d("9999999"))).isEqualByComparingTo("15000");
    }

    @Test
    void pickupIsAlwaysFree() {
        assertThat(calculator("15000", null).fee(FulfillmentType.PICKUP, d("1000"))).isEqualByComparingTo("0");
    }
}
