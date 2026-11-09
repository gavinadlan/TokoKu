import { useId, type ReactElement, type ReactNode, cloneElement } from 'react';

interface Props {
  label: string;
  error?: string;
  hint?: string;
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>;
  extra?: ReactNode;
}

/** Label + kontrol + pesan error, dengan id yang saling terhubung (aksesibilitas formulir). */
export function Field({ label, error, hint, children, extra }: Props) {
  const id = useId();
  const describedBy = [error ? `${id}-err` : null, hint ? `${id}-hint` : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })}
      {hint && !error && (
        <span className="hint" id={`${id}-hint`}>
          {hint}
        </span>
      )}
      {error && (
        <span className="field-error" id={`${id}-err`} role="alert">
          {error}
        </span>
      )}
      {extra}
    </div>
  );
}
