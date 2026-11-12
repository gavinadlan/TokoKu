package com.tokoku.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Warna dan jenis font untuk email dan invoice, dari theme/tema.json (berkas yang sama dengan frontend lewat
 * scripts/palet.mjs). Varian dipilih dengan env APP_THEME / properti sistem app.theme; kosong = "aktif" di berkas.
 */
public final class Theme {

    private static final JsonNode ROOT = load();
    private static final Theme CURRENT = of(System.getProperty("app.theme", System.getenv("APP_THEME")));

    private final Map<String, String> v;

    private Theme(Map<String, String> v) { this.v = v; }

    public static Theme current() { return CURRENT; }

    public static Theme of(String name) {
        String n = name == null || name.isBlank() ? ROOT.get("aktif").asString() : name;
        JsonNode all = ROOT.get("varian");
        if (!all.has(n)) throw new IllegalArgumentException("Unknown theme: " + n);
        Map<String, String> m = new HashMap<>();
        all.get("base").properties().forEach(e -> m.put(e.getKey(), e.getValue().asString()));
        all.get(n).properties().forEach(e -> m.put(e.getKey(), e.getValue().asString()));
        return new Theme(m);
    }

    public String get(String key) { return v.get(key); }

    public boolean sansDisplay() { return "sans".equals(v.get("display")); }

    private static JsonNode load() {
        try (InputStream in = new ClassPathResource("theme/tema.json").getInputStream()) {
            return JsonMapper.builder().build().readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read theme/tema.json", e);
        }
    }
}
