import { cloneElement, isValidElement, useId } from 'react';

/** Label + control + hint/error. Wires id, aria-invalid and aria-describedby onto the child control. */
export function FormField({ label, required, error, hint, className = '', children }) {
  const id = useId();
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined;
  const control = isValidElement(children)
    ? cloneElement(children, {
        id,
        'aria-invalid': error ? 'true' : undefined,
        'aria-describedby': describedBy,
      })
    : children;
  return (
    <div className={`field ${className}`}>
      {label && (
        <label className="field-label" htmlFor={id}>
          {label}
          {required && <span className="req" aria-hidden="true">*</span>}
        </label>
      )}
      {control}
      {error ? (
        <span className="field-error" id={`${id}-error`}>
          {error}
        </span>
      ) : (
        hint && (
          <span className="field-hint" id={`${id}-hint`}>
            {hint}
          </span>
        )
      )}
    </div>
  );
}
