import { useCallback, useState } from 'react';
import { buildPayload, fieldProps, validatePayload } from '../utils/contract';

/**
 * Form state bound to an OpenAPI request schema.
 *  - bind(field) returns input props with the schema's constraints (maxLength, min, max…)
 *  - validate() builds the typed payload (schema fields only) and returns it, or null if invalid
 *  - applyServerErrors(apiError) maps backend field errors onto the form
 */
export function useContractForm(schemaName, initialValues = {}) {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});

  const set = useCallback((field, value) => {
    setValues((v) => ({ ...v, [field]: value }));
    setErrors((e) => (e[field] ? { ...e, [field]: undefined } : e));
  }, []);

  const bind = (field) => {
    const { required, ...props } = fieldProps(schemaName, field);
    return {
      ...props,
      'aria-required': required || undefined,
      value: values[field] ?? '',
      onChange: (e) => set(field, e.target.value),
    };
  };

  const bindCheckbox = (field) => ({
    name: field,
    checked: Boolean(values[field]),
    onChange: (e) => set(field, e.target.checked),
  });

  const validate = (overrides = {}) => {
    const payload = buildPayload(schemaName, { ...values, ...overrides });
    const errs = validatePayload(schemaName, payload);
    setErrors(errs);
    return Object.keys(errs).length ? null : payload;
  };

  const applyServerErrors = (apiError) => {
    if (apiError?.fieldErrors && Object.keys(apiError.fieldErrors).length) {
      setErrors((e) => ({ ...e, ...apiError.fieldErrors }));
    }
  };

  return { values, set, setValues, errors, setErrors, bind, bindCheckbox, validate, applyServerErrors };
}
