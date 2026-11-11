package com.tokoku.store;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Identitas toko CONTOH, hanya di profil dev. Di produksi pengaturan harus diisi admin (invoice 409 sebelum itu). */
@Component
@Profile("dev")
public class StoreSettingsDevSeeder implements ApplicationRunner {

    private final StoreService service;

    public StoreSettingsDevSeeder(StoreService service) {
        this.service = service;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        StoreSettings s = service.current();
        if (s.isConfigured()) {
            return; // idempoten: jangan menimpa isian admin
        }
        s.update("TokoKu", "Jl. Contoh No. 1, Kota Anda", "0800-0000-0000", "halo@tokoku.example",
                "Senin sampai Sabtu, 09.00 sampai 17.00",
                "Tunjukkan kode ambil kepada petugas di kasir. Bawa identitas bila diminta.",
                true, true, new BigDecimal("15000"), new BigDecimal("500000"),
                "Terima kasih telah berbelanja di TokoKu. Simpan invoice ini sebagai bukti pembayaran.", null, null);
    }
}
