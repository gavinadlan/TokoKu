-- Penghitung nomor invoice per bulan; satu baris per periode (YYYYMM), dikunci saat dinaikkan
CREATE TABLE invoice_counters (
    period     CHAR(6) PRIMARY KEY,
    last_value INTEGER NOT NULL
);

ALTER TABLE orders
    ADD COLUMN invoice_number VARCHAR(20) UNIQUE,
    ADD COLUMN invoiced_at    TIMESTAMPTZ;

-- Metode pembayaran dari notifikasi Midtrans (payment_type), untuk ditampilkan di invoice
ALTER TABLE payments ADD COLUMN payment_type VARCHAR(40);
