-- Sidik bcrypt dari ADMIN_PASSWORD terakhir yang dilihat AdminBootstrap; untuk mendeteksi env yang diubah. NULL = belum pernah dicatat.
ALTER TABLE users ADD COLUMN bootstrap_env_hash VARCHAR(100);
