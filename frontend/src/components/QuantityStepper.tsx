interface Props {
  value: number;
  min?: number;
  max: number;
  onChange(value: number): void;
  label: string;
  disabled?: boolean;
}

/** Stepper jumlah: tombol 44px, angka tabular, batas stok dijelaskan lewat teks oleh pemanggil. */
export function QuantityStepper({ value, min = 1, max, onChange, label, disabled }: Props) {
  return (
    <div className="stepper" role="group" aria-label={label}>
      <button type="button" aria-label="Kurangi jumlah" disabled={disabled || value <= min} onClick={() => onChange(value - 1)}>
        <span aria-hidden="true">&minus;</span>
      </button>
      <output className="tnum" aria-live="polite">
        {value}
      </output>
      <button type="button" aria-label="Tambah jumlah" disabled={disabled || value >= max} onClick={() => onChange(value + 1)}>
        <span aria-hidden="true">+</span>
      </button>
    </div>
  );
}
