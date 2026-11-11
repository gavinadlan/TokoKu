package com.tokoku.image;

/**
 * Empat ukuran bernama. Variasi transformasi dibatasi di sini karena kredit Cloudinary gratis terbatas.
 * Tiga ukuran kecil dipotong 4:5 dengan lebar tertentu; LARGE (lightbox, layar rapat piksel) tidak dipotong:
 * rasio asli, sisi terpanjang 1600px, tidak diperbesar.
 */
public enum ImageSize {
    THUMBNAIL(160, true), CARD(480, true), DETAIL(960, true), LARGE(1600, false);

    private final int width;
    private final boolean crop;

    ImageSize(int width, boolean crop) { this.width = width; this.crop = crop; }

    /** Lebar px untuk ukuran terpotong; batas sisi terpanjang untuk ukuran utuh. */
    public int width() { return width; }

    /** True: potong 4:5 (c_fill). False: muat dalam kotak width x width tanpa potong (c_limit). */
    public boolean crop() { return crop; }
}
