import { useId, type ReactElement, type ReactNode, cloneElement } from 'react';

interface Props {
  label: string;
  /** Id kontrol bila kontrol dibungkus (mis. Controller): Field tidak menyuntikkan id, pemanggil memasangnya sendiri. */
  htmlFor?: string;
  error?: string;
  hint?: string;
  children: ReactElement<{ id?: string; 'aria-invalid'?: boolean; 'aria-describedby'?: string }>;
  extra?: ReactNode;
}

/** Label + kontrol + pesan error, dengan id yang saling terhubung (aksesibilitas formulir). */
export function Field({ label, htmlFor, error, hint, children, extra }: Props) {
  const generated = useId();
  const id = htmlFor ?? generated;
  const describedBy = [error ? `${id}-err` : null, hint ? `${id}-hint` : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {htmlFor ? children : cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })}
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
