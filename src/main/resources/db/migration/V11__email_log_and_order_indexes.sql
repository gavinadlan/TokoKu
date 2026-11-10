-- Satu baris per (order, jenis email): unique = idempotensi (webhook ganda tidak mengirim dobel)
CREATE TABLE email_log (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT      NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    kind       VARCHAR(30) NOT NULL,
    status     VARCHAR(10) NOT NULL CHECK (status IN ('SENDING', 'SENT', 'FAILED')),
    attempts   INT         NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at    TIMESTAMPTZ,
    CONSTRAINT uq_email_log_order_kind UNIQUE (order_id, kind)
);

-- Ringkasan admin (polling): hitung order dibayar sejak waktu tertentu dan order berstatus PAID
CREATE INDEX idx_orders_invoiced_at ON orders (invoiced_at) WHERE invoiced_at IS NOT NULL;
