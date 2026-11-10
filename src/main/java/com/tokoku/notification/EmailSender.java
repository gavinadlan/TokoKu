package com.tokoku.notification;

/** Pengirim email. Ganti implementasi (SMTP lain, API provider) tanpa menyentuh logika pesanan. */
public interface EmailSender {

    /** false = tidak mengirim apa pun (tanpa kredensial / dimatikan); pemanggil melewati pekerjaan dan tidak mencatat email_log. */
    default boolean delivers() { return true; }

    /** Melempar RuntimeException bila gagal; pemanggil yang mengulang. */
    void send(EmailMessage message);

    /** "budi@example.com" -> "bu***@example.com": untuk log, tidak pernah alamat penuh. */
    static String mask(String email) {
        if (email == null) return "-";
        int at = email.indexOf('@');
        if (at < 1) return "***";
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
