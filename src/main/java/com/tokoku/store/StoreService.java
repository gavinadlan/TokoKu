package com.tokoku.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tokoku.store.StoreDtos.AdminStoreResponse;
import com.tokoku.store.StoreDtos.PublicStoreResponse;
import com.tokoku.store.StoreDtos.StoreSettingsRequest;

import java.math.BigDecimal;

@Service
public class StoreService {

    private final StoreSettingsRepository repository;

    public StoreService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StoreSettings current() {
        return repository.findById(StoreSettings.ID).orElseThrow(() -> new IllegalStateException("store_settings row missing"));
    }

    @Transactional(readOnly = true)
    public PublicStoreResponse publicView() { return PublicStoreResponse.from(current()); }

    @Transactional(readOnly = true)
    public AdminStoreResponse adminView() { return AdminStoreResponse.from(current()); }

    @Transactional
    public AdminStoreResponse update(StoreSettingsRequest r) {
        StoreSettings s = current();
        s.update(r.name().trim(), r.address().trim(), r.phone().trim(), blank(r.email()), blank(r.openingHours()),
                blank(r.pickupInstructions()), r.deliveryEnabled(), r.pickupEnabled(),
                r.flatShippingFee() == null ? BigDecimal.ZERO : r.flatShippingFee(), r.freeShippingThreshold(),
                blank(r.invoiceFooter()), nullIfBlank(r.taxId()), nullIfBlank(r.taxNote()));
        return AdminStoreResponse.from(s);
    }

    private static String blank(String s) { return s == null ? "" : s.trim(); }

    private static String nullIfBlank(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
