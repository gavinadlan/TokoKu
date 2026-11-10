-- Rekonsiliasi berkala (Status API): hanya payment PENDING terbaru yang dibaca
CREATE INDEX idx_payments_pending_created ON payments (created_at) WHERE status = 'PENDING';
