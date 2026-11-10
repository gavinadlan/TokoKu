package com.tokoku.order;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tokoku.store.StoreService;
import com.tokoku.store.StoreSettings;

/** Tarif flat dari pengaturan toko; gratis jika subtotal mencapai ambang (jika diatur); ambil di toko selalu gratis. */
@Component
public class FlatRateShippingCalculator implements ShippingCalculator {

    private final StoreService store;

    public FlatRateShippingCalculator(StoreService store) {
        this.store = store;
    }

    @Override
    public BigDecimal fee(FulfillmentType type, BigDecimal subtotal) {
        if (type == FulfillmentType.PICKUP) {
            return BigDecimal.ZERO;
        }
        StoreSettings s = store.current();
        BigDecimal threshold = s.getFreeShippingThreshold();
        if (threshold != null && subtotal.compareTo(threshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return s.getFlatShippingFee();
    }
}
