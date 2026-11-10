package com.tokoku.order;

import java.math.BigDecimal;

/** Menghitung ongkir di server. Implementasi lain (zona, API kurir) bisa menyusul lewat antarmuka yang sama. */
public interface ShippingCalculator {
    BigDecimal fee(FulfillmentType type, BigDecimal subtotal);
}
