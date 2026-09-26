// Request payloads and client-side validation are derived from openapi.json's component schemas,
// so a form can only send fields the contract defines, with the constraints the contract declares.
import { components } from '../../openapi.json';

const { schemas } = components;

export function getSchema(name) {
  const schema = schemas[name];
  if (!schema) throw new Error(`Unknown schema ${name}`);
  return schema;
}

export const isRequired = (schemaName, field) => (getSchema(schemaName).required ?? []).includes(field);

export function fieldProps(schemaName, field) {
  const prop = getSchema(schemaName).properties?.[field];
  if (!prop) throw new Error(`${schemaName}.${field} is not in the contract`);
  const props = { name: field, required: isRequired(schemaName, field) };
  if (prop.maxLength) props.maxLength = prop.maxLength;
  if (prop.minLength) props.minLength = prop.minLength;
  if (prop.minimum !== undefined) props.min = prop.minimum;
  if (prop.maximum !== undefined) props.max = prop.maximum;
  if (prop.type === 'integer') props.step = 1;
  if (prop.type === 'number') props.step = 'any';
  return props;
}

const isEmpty = (v) => v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0);

/**
 * Builds a request body containing only properties declared in the schema, converting form strings
 * to the declared types. Empty optional values are omitted.
 */
export function buildPayload(schemaName, values) {
  const { properties = {} } = getSchema(schemaName);
  const payload = {};
  for (const [key, prop] of Object.entries(properties)) {
    let value = values[key];
    if (typeof value === 'string') value = value.trim();
    if (isEmpty(value)) continue;
    if (prop.type === 'integer' || prop.type === 'number') value = Number(value);
    if (prop.type === 'array' && prop.items?.type === 'integer') value = value.map(Number);
    payload[key] = value;
  }
  return payload;
}

/** Validates a built payload against the schema's documented constraints. Returns { field: message }. */
export function validatePayload(schemaName, payload) {
  const { properties = {}, required = [] } = getSchema(schemaName);
  const errors = {};
  for (const field of required) {
    if (isEmpty(payload[field])) errors[field] = 'This field is required.';
  }
  for (const [field, prop] of Object.entries(properties)) {
    const value = payload[field];
    if (isEmpty(value) || errors[field]) continue;
    if (prop.type === 'string') {
      if (prop.minLength && value.length < prop.minLength) errors[field] = `Must be at least ${prop.minLength} characters.`;
      else if (prop.maxLength !== undefined && value.length > prop.maxLength) errors[field] = `Must be at most ${prop.maxLength} characters.`;
      else if (prop.pattern && !new RegExp(`^(?:${prop.pattern})$`).test(value)) errors[field] = 'Invalid format.';
    }
    if (prop.type === 'integer' || prop.type === 'number') {
      if (Number.isNaN(value)) errors[field] = 'Must be a number.';
      else if (prop.type === 'integer' && !Number.isInteger(value)) errors[field] = 'Must be a whole number.';
      else if (prop.minimum !== undefined && value < prop.minimum) errors[field] = `Must be ≥ ${prop.minimum}.`;
      else if (prop.maximum !== undefined && value > prop.maximum) errors[field] = `Must be ≤ ${prop.maximum}.`;
    }
  }
  return errors;
}
