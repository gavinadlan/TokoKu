package com.tokoku.auth;

final class AuthRules {

    private AuthRules() {}

    /** Realm toko hanya untuk USER; realm admin hanya untuk ADMIN. */
    static boolean allows(User user, Realm realm) {
        return realm == Realm.ADMIN ? user.getRole() == Role.ADMIN : user.getRole() == Role.USER;
    }
}
