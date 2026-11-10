package com.tokoku.image;

/** Penyimpanan gambar: Cloudinary untuk sungguhan, Fake untuk test dan dev tanpa kunci. */
public interface ImageStorage {

    String FOLDER = "tokoku/products";
    String ALLOWED_FORMATS = "jpg,png,webp";

    /** Tanda tangan unggah langsung dari browser. Rahasia API tidak pernah dikirim ke klien. */
    UploadSignature signUpload();

    /** True jika respons unggah benar-benar berasal dari penyimpanan (signature public_id + version cocok). */
    boolean verifyUploadResponse(String publicId, long version, String signature);

    /** URL asli gambar yang disimpan di database. Dibangun server, bukan diterima dari klien. */
    String originalUrl(String publicId, long version, String format);

    /** URL ukuran bernama. Gambar eksternal (tanpa publicId) dikembalikan apa adanya. */
    String variantUrl(String storedUrl, String publicId, ImageSize size);

    /** Hapus aset, best-effort: tidak pernah melempar exception (kegagalan hanya dicatat). */
    void delete(String publicId);
}
