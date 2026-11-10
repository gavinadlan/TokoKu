package com.tokoku.invoice;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nomor invoice INV-YYYYMM-NNNNN. Penghitung per bulan dinaikkan dengan satu statement atomik
 * (INSERT ... ON CONFLICT DO UPDATE ... RETURNING): baris dikunci sampai transaksi commit, jadi dua pembayaran
 * bersamaan tidak pernah mendapat nomor yang sama, dan transaksi yang gagal tidak meninggalkan celah nomor.
 */
@Service
public class InvoiceNumberService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMM");

    private final JdbcTemplate jdbc;
    private final ZoneId zone;

    public InvoiceNumberService(JdbcTemplate jdbc, @Value("${app.timezone:Asia/Jakarta}") String timezone) {
        this.jdbc = jdbc;
        this.zone = ZoneId.of(timezone);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        String period = ZonedDateTime.now(zone).format(PERIOD);
        Integer n = jdbc.queryForObject(
                "INSERT INTO invoice_counters (period, last_value) VALUES (?, 1) "
                        + "ON CONFLICT (period) DO UPDATE SET last_value = invoice_counters.last_value + 1 RETURNING last_value",
                Integer.class, period);
        return "INV-" + period + "-" + String.format("%05d", n);
    }
}
