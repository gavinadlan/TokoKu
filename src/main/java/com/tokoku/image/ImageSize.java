package com.tokoku.image;

/** Tiga ukuran bernama (lebar px, rasio 4:5). Variasi transformasi dibatasi di sini karena kredit Cloudinary gratis terbatas. */
public enum ImageSize {
    THUMBNAIL(160), CARD(480), DETAIL(960);

    private final int width;

    ImageSize(int width) { this.width = width; }

    public int width() { return width; }
}
