package com.tokoku.store;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

/** Satu baris pengaturan toko (id selalu 1). */
@Entity
@Table(name = "store_settings")
public class StoreSettings {

    public static final short ID = 1;

    @Id
    private Short id = ID;

    private String name;
    private String address;
    private String phone;
    private String email;

    @Column(name = "opening_hours")
    private String openingHours;

    @Column(name = "pickup_instructions")
    private String pickupInstructions;

    @Column(name = "delivery_enabled")
    private boolean deliveryEnabled = true;

    @Column(name = "pickup_enabled")
    private boolean pickupEnabled = true;

    @Column(name = "flat_shipping_fee")
    private BigDecimal flatShippingFee = BigDecimal.ZERO;

    @Column(name = "free_shipping_threshold")
    private BigDecimal freeShippingThreshold;

    @Column(name = "invoice_footer")
    private String invoiceFooter;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "tax_note")
    private String taxNote;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected StoreSettings() {}

    /** Identitas dianggap lengkap jika nama, alamat, dan telepon terisi. Tanpa itu invoice tidak boleh diterbitkan. */
    public boolean isConfigured() {
        return notBlank(name) && notBlank(address) && notBlank(phone);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }

    public void update(String name, String address, String phone, String email, String openingHours, String pickupInstructions,
            boolean deliveryEnabled, boolean pickupEnabled, BigDecimal flatShippingFee, BigDecimal freeShippingThreshold,
            String invoiceFooter, String taxId, String taxNote) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.openingHours = openingHours;
        this.pickupInstructions = pickupInstructions;
        this.deliveryEnabled = deliveryEnabled;
        this.pickupEnabled = pickupEnabled;
        this.flatShippingFee = flatShippingFee;
        this.freeShippingThreshold = freeShippingThreshold;
        this.invoiceFooter = invoiceFooter;
        this.taxId = taxId;
        this.taxNote = taxNote;
        this.updatedAt = Instant.now();
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getOpeningHours() { return openingHours; }
    public String getPickupInstructions() { return pickupInstructions; }
    public boolean isDeliveryEnabled() { return deliveryEnabled; }
    public boolean isPickupEnabled() { return pickupEnabled; }
    public BigDecimal getFlatShippingFee() { return flatShippingFee; }
    public BigDecimal getFreeShippingThreshold() { return freeShippingThreshold; }
    public String getInvoiceFooter() { return invoiceFooter; }
    public String getTaxId() { return taxId; }
    public String getTaxNote() { return taxNote; }
}
