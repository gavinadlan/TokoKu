package com.tokoku.auth;

/** Ruang akses token. Token toko tidak bisa memanggil endpoint admin, dan sebaliknya. */
public enum Realm {
    STORE("store"), ADMIN("admin");

    private final String claim;

    Realm(String claim) { this.claim = claim; }

    public String claim() { return claim; }

    public String authority() { return "REALM_" + claim; }
}
