package com.tokoku.store;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class StoreDtos {

    private StoreDtos() {}

    /** Hanya field yang aman ditampilkan publik. */
    public record PublicStoreResponse(String name, String address, String phone, String email, String openingHours,
            String pickupInstructions, boolean deliveryEnabled, boolean pickupEnabled, BigDecimal flatShippingFee,
            BigDecimal freeShippingThreshold, boolean configured) {

        static PublicStoreResponse from(StoreSettings s) {
            return new PublicStoreResponse(s.getName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getOpeningHours(),
                    s.getPickupInstructions(), s.isDeliveryEnabled(), s.isPickupEnabled() && s.isConfigured(),
                    s.getFlatShippingFee(), s.getFreeShippingThreshold(), s.isConfigured());
        }
    }

    public record AdminStoreResponse(String name, String address, String phone, String email, String openingHours,
            String pickupInstructions, boolean deliveryEnabled, boolean pickupEnabled, BigDecimal flatShippingFee,
            BigDecimal freeShippingThreshold, String invoiceFooter, String taxId, String taxNote, boolean configured) {

        static AdminStoreResponse from(StoreSettings s) {
            return new AdminStoreResponse(s.getName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getOpeningHours(),
                    s.getPickupInstructions(), s.isDeliveryEnabled(), s.isPickupEnabled(), s.getFlatShippingFee(),
                    s.getFreeShippingThreshold(), s.getInvoiceFooter(), s.getTaxId(), s.getTaxNote(), s.isConfigured());
        }
    }

    public record StoreSettingsRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 300) String address,
            @NotBlank @Size(max = 40) String phone,
            @Email @Size(max = 150) String email,
            @Size(max = 200) String openingHours,
            @Size(max = 500) String pickupInstructions,
            boolean deliveryEnabled,
            boolean pickupEnabled,
            @DecimalMin("0") BigDecimal flatShippingFee,
            @DecimalMin("0") BigDecimal freeShippingThreshold,
            @Size(max = 500) String invoiceFooter,
            @Size(max = 60) String taxId,
            @Size(max = 200) String taxNote) {}
}
