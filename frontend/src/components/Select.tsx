import * as RadixSelect from '@radix-ui/react-select';
import { useCoarsePointer } from '../lib/useCoarsePointer';

export interface SelectOption {
  value: string;
  label: string;
}

interface Props {
  id?: string;
  value: string;
  onChange(value: string): void;
  options: SelectOption[];
  /** Nama aksesibel bila tidak ada <label htmlFor>. */
  ariaLabel?: string;
  disabled?: boolean;
  placeholder?: string;
  invalid?: boolean;
  className?: string;
}

/**
 * Select kustom (Radix, headless): panah, Home/End, ketik huruf awal, Escape, fokus kembali ke pemicu.
 * Di perangkat sentuh dipakai <select> native yang bergaya sama (pemilih bawaan ponsel lebih nyaman).
 */
export function Select({ id, value, onChange, options, ariaLabel, disabled, placeholder, invalid, className = '' }: Props) {
  const coarse = useCoarsePointer();

  if (coarse) {
    return (
      <select id={id} className={`select-native ${className}`} value={value} aria-label={ariaLabel} aria-invalid={invalid || undefined} disabled={disabled} onChange={(e) => onChange(e.target.value)}>
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    );
  }

  return (
    <RadixSelect.Root value={value === '' ? undefined : value} onValueChange={onChange} disabled={disabled}>
      <RadixSelect.Trigger id={id} className={`sel-trigger ${className}`} aria-label={ariaLabel} aria-invalid={invalid || undefined}>
        <RadixSelect.Value placeholder={placeholder ?? 'Pilih'} />
        <RadixSelect.Icon asChild>
          <span className="sel-chev" aria-hidden="true" />
        </RadixSelect.Icon>
      </RadixSelect.Trigger>
      <RadixSelect.Portal>
        <RadixSelect.Content className="pop sel-content" position="popper" sideOffset={4} collisionPadding={8}>
          <RadixSelect.Viewport>
            {options.map((o) => (
              <RadixSelect.Item key={o.value} value={o.value} className="pop-item">
                <RadixSelect.ItemText>{o.label}</RadixSelect.ItemText>
                <RadixSelect.ItemIndicator asChild>
                  <span className="sel-check" aria-hidden="true" />
                </RadixSelect.ItemIndicator>
              </RadixSelect.Item>
            ))}
          </RadixSelect.Viewport>
        </RadixSelect.Content>
      </RadixSelect.Portal>
    </RadixSelect.Root>
  );
}
