-- Pengaturan toko: satu baris (singleton). Identitas kosong = belum dikonfigurasi (invoice 409 sampai admin mengisi).
CREATE TABLE store_settings (
    id                      SMALLINT PRIMARY KEY CHECK (id = 1),
    name                    VARCHAR(100)   NOT NULL,
    address                 VARCHAR(300)   NOT NULL DEFAULT '',
    phone                   VARCHAR(40)    NOT NULL DEFAULT '',
    email                   VARCHAR(150)   NOT NULL DEFAULT '',
    opening_hours           VARCHAR(200)   NOT NULL DEFAULT '',
    pickup_instructions     VARCHAR(500)   NOT NULL DEFAULT '',
    delivery_enabled        BOOLEAN        NOT NULL DEFAULT TRUE,
    pickup_enabled          BOOLEAN        NOT NULL DEFAULT TRUE,
    flat_shipping_fee       NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (flat_shipping_fee >= 0),
    free_shipping_threshold NUMERIC(12, 2) CHECK (free_shipping_threshold >= 0),
    invoice_footer          VARCHAR(500)   NOT NULL DEFAULT '',
    tax_id                  VARCHAR(60),
    tax_note                VARCHAR(200),
    updated_at              TIMESTAMPTZ    NOT NULL DEFAULT now()
);
INSERT INTO store_settings (id, name) VALUES (1, 'TokoKu');

ALTER TABLE orders
    ADD COLUMN fulfillment_type VARCHAR(10)    NOT NULL DEFAULT 'DELIVERY' CHECK (fulfillment_type IN ('DELIVERY', 'PICKUP')),
    ADD COLUMN shipping_fee     NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (shipping_fee >= 0),
    ADD COLUMN courier          VARCHAR(50),
    ADD COLUMN tracking_number  VARCHAR(60),
    ADD COLUMN pickup_code      VARCHAR(12),
    ADD COLUMN pickup_ready_at  TIMESTAMPTZ;

-- Alamat hanya wajib untuk DELIVERY (ditegakkan di aplikasi dan constraint ini)
ALTER TABLE orders ALTER COLUMN shipping_address DROP NOT NULL;
ALTER TABLE orders ADD CONSTRAINT orders_delivery_needs_address
    CHECK (fulfillment_type = 'PICKUP' OR shipping_address IS NOT NULL);

ALTER TABLE orders DROP CONSTRAINT orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN
    ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED'));

CREATE UNIQUE INDEX uq_orders_pickup_code ON orders (pickup_code) WHERE pickup_code IS NOT NULL;
