-- Realm token: 'store' (pembeli) atau 'admin'. Refresh token tidak bisa dipakai lintas realm.
ALTER TABLE refresh_tokens ADD COLUMN realm VARCHAR(10) NOT NULL DEFAULT 'store' CHECK (realm IN ('store', 'admin'));
