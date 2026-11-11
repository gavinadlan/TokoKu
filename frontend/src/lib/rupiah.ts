const grouping = new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 });

/** "1450000" -> "1.450.000" (tanpa awalan Rp; awalan ada di komponen) */
export function formatThousands(value: number): string {
  return grouping.format(value);
}

/** Hanya digit yang dipakai: "Rp 85.000,5" -> 850005. Kosong -> undefined. Dibatasi 10 digit. */
export function parseRupiahInput(text: string): number | undefined {
  const digits = text.replace(/\D/g, '').slice(0, 10);
  return digits === '' ? undefined : Number(digits);
}

/** Posisi kursor baru setelah pemformatan: menjaga jumlah digit di sebelah kiri kursor. */
export function caretAfterFormat(rawBeforeCaret: string, formatted: string): number {
  const digitsBefore = rawBeforeCaret.replace(/\D/g, '').length;
  if (digitsBefore === 0) return 0;
  let seen = 0;
  for (let i = 0; i < formatted.length; i++) {
    if (/\d/.test(formatted[i]!)) seen++;
    if (seen === digitsBefore) return i + 1;
  }
  return formatted.length;
}
