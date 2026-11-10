import { forwardRef, useLayoutEffect, useRef, type ChangeEvent } from 'react';
import { caretAfterFormat, formatThousands, parseRupiahInput } from '../lib/rupiah';

interface Props {
  id?: string;
  value: number | undefined;
  onChange(value: number | undefined): void;
  onBlur?(): void;
  invalid?: boolean;
  describedBy?: string;
  disabled?: boolean;
}

/** Input harga: awalan "Rp", pemisah ribuan saat mengetik, rata kanan, hanya digit; nilai berupa angka bulat. */
export const RupiahInput = forwardRef<HTMLInputElement, Props>(function RupiahInput({ id, value, onChange, onBlur, invalid, describedBy, disabled }, ref) {
  const local = useRef<HTMLInputElement | null>(null);
  const caret = useRef<number | null>(null);
  const text = value === undefined ? '' : formatThousands(value);

  useLayoutEffect(() => {
    if (caret.current !== null && local.current && document.activeElement === local.current) {
      local.current.setSelectionRange(caret.current, caret.current);
    }
    caret.current = null;
  });

  const handle = (e: ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    const next = parseRupiahInput(raw);
    const formatted = next === undefined ? '' : formatThousands(next);
    caret.current = caretAfterFormat(raw.slice(0, e.target.selectionStart ?? raw.length), formatted);
    onChange(next);
  };

  return (
    <span className={`price-in ${invalid ? 'is-err' : ''}`}>
      <span aria-hidden="true">Rp</span>
      <input
        id={id}
        ref={(el) => {
          local.current = el;
          if (typeof ref === 'function') ref(el);
          else if (ref) ref.current = el;
        }}
        inputMode="numeric"
        autoComplete="off"
        value={text}
        placeholder="0"
        onChange={handle}
        onBlur={onBlur}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        disabled={disabled}
      />
    </span>
  );
});
