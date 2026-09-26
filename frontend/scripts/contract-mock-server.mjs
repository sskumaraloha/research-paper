// Contract mock server: serves every operation in openapi.json with schema-shaped sample data and
// REJECTS any request that deviates from the contract (unknown path/method, undocumented or missing
// query params, request-body fields not in the schema, missing required fields, constraint violations,
// missing bearer token). Used for local development without the backend and for integration testing.
//
//   node scripts/contract-mock-server.mjs [port]      (default 8080)
//
// Every request is logged; GET /__mock/violations lists contract violations seen, /__mock/log the requests,
// /__mock/coverage the operations exercised, and /__mock/reset clears the log and violations.
import { createServer } from 'node:http';
import { readFileSync } from 'node:fs';

const spec = JSON.parse(readFileSync(new URL('../openapi.json', import.meta.url), 'utf8'));
const schemas = spec.components.schemas;
const port = Number(process.argv[2] ?? process.env.PORT ?? 8080);
const PUBLIC = new Set(['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/demo-login', '/api/auth/forgot-password', '/api/auth/reset-password', '/api/auth/reset-password/validate']);

const ops = [];
for (const [path, methods] of Object.entries(spec.paths)) {
  for (const [method, op] of Object.entries(methods)) {
    ops.push({ path, method: method.toUpperCase(), op, regex: new RegExp('^' + path.replace(/\{[^}]+\}/g, '([^/]+)') + '$') });
  }
}
ops.sort((a, b) => (a.path.match(/\{/g) ?? []).length - (b.path.match(/\{/g) ?? []).length);

const resolve = (s) => (s?.$ref ? schemas[s.$ref.split('/').pop()] : s);

// ---------- sample data generation ----------
const WORDS = {
  name: ['Hydraulic Press 2', 'CNC Lathe A', 'Packaging Line Conveyor', 'Injection Moulder 5', 'Air Compressor'],
  fullName: ['Priya Nair', 'Tom Becker', 'Aisha Khan', 'Luis Ortega'],
  machineName: ['Hydraulic Press 2', 'CNC Lathe A', 'Conveyor C3', 'Moulder 5', 'Compressor 1'],
  failureMode: ['Hydraulic leak', 'Bearing failure', 'Belt wear', 'Electrical fault', 'Overheating'],
  lineName: ['Line 1 – Stamping', 'Line 2 – Assembly', 'Line 3 – Packing'],
  partName: ['Seal kit 40mm', 'Bearing 6204', 'V-belt A42', 'Contactor 24V'],
  title: ['Quarterly hydraulic inspection', 'Replace filters', 'Lubricate bearings', 'Repeated hydraulic leaks on Press 2'],
  description: ['Oil leak at main cylinder, pressure dropping during cycle.', 'Unusual noise from spindle bearing.', 'Belt slipping under load.'],
  actionTaken: ['Replaced seal kit and topped up oil.', 'Replaced bearing, realigned shaft.'],
  technician: ['R. Singh', 'M. Weber', 'J. Park'],
  status: ['ACTIVE', 'PENDING', 'COMPLETED'],
  role: ['ADMIN', 'ENGINEER', 'TECHNICIAN'],
  criticality: ['HIGH', 'MEDIUM', 'LOW'],
  severity: ['HIGH', 'MEDIUM', 'LOW'],
  category: ['Mechanical', 'Electrical', 'Hydraulic'],
  source: ['MANUAL', 'IMPORT', 'ASSISTANT'],
  sender: ['USER', 'AGENT'],
  label: ['Downtime (h)', 'Records', 'MTBF (days)', 'Open validations'],
  heading: ['Summary', 'Likely causes', 'Recommendation'],
  message: ['Import finished with 4 rows needing review.', 'Machine Press 2 exceeded the downtime alert.'],
  content: ['Press 2 leaked oil yesterday, replaced seals, 90 minutes down.', 'Got it — which machine was this on?'],
  filename: ['maintenance_log_2025.xlsx', 'press_repairs.csv'],
  code: ['PRS-02', 'CNC-A', 'CNV-03'],
  partNumber: ['SK-40', '6204-2RS', 'A42'],
  email: ['priya@example.com', 'tom@example.com'],
  phoneNumber: ['+4915112345678', '+919812345678'],
  plantName: ['Pune Plant', 'Leipzig Plant'],
  organisationName: ['Acme Manufacturing'],
  location: ['Pune, IN', 'Leipzig, DE'],
  yearMonth: ['2025-04', '2025-05', '2025-06', '2025-07', '2025-08', '2025-09'],
  rawText: ['press #2', 'PRS2', 'hyd press two'],
  snippet: ['…hydraulic <b>leak</b> at main cylinder…'],
  detail: ['5 hydraulic leak events in 30 days, up from 1 in the previous period.'],
  question: ['Which machines had the most downtime?'],
};
const pick = (list, i) => list[i % list.length];

function sample(schema, key = '', i = 0, depth = 0) {
  schema = resolve(schema);
  if (!schema || depth > 6) return null;
  if (schema.type === 'array') {
    const n = key === 'missingFields' ? 1 : key === 'reasons' ? 2 : key === 'yearMonth' ? 6 : key === 'points' ? 6 : 3;
    return Array.from({ length: n }, (_, j) => sample(schema.items, key.replace(/s$/, ''), i * 3 + j, depth + 1));
  }
  if (schema.type === 'object' || schema.properties) {
    if (schema.additionalProperties && !schema.properties) {
      return key === 'rawData'
        ? { Machine: 'press #2', Date: '2025-09-01', Issue: 'hyd leak', Minutes: '90' }
        : { equipment: { press: ['prs', 'press machine'], conveyor: ['belt line'] }, failure: { leak: ['leaking', 'oil loss'] } };
    }
    const obj = {};
    for (const [k, v] of Object.entries(schema.properties ?? {})) obj[k] = sample(v, k, i, depth + 1);
    return obj;
  }
  if (schema.type === 'integer') {
    if (/id$/i.test(key) || key === 'id') return 100 + i;
    if (key === 'page') return 0;
    if (key === 'size') return 20;
    if (key === 'totalPages') return 3;
    if (key === 'totalElements') return 57;
    if (/minutes/i.test(key)) return [90, 45, 240, 30, 120, 60][i % 6] * 7;
    if (key === 'daysUntilDue') return [-2, 3, 10][i % 3];
    return [12, 7, 23, 4, 15][i % 5];
  }
  if (schema.type === 'number') {
    if (/confidence|threshold/i.test(key)) return [0.62, 0.48, 0.91][i % 3];
    if (/pct/i.test(key)) return [38.5, 24.1, 12.7][i % 3];
    return [12.4, 8.2, 31.6][i % 3];
  }
  if (schema.type === 'boolean') return key === 'read' ? i % 2 === 0 : key !== 'last';
  if (schema.format === 'date') return `2025-09-${String(1 + (i % 28)).padStart(2, '0')}`;
  if (schema.format === 'date-time') return `2025-09-${String(1 + (i % 28)).padStart(2, '0')}T08:30:00Z`;
  if (key === 'accessToken' || key === 'refreshToken') return `mock-${key}-${Date.now()}`;
  if (key === 'value') return String([42, 7, 3][i % 3]);
  if (key === 'unit') return ['h', '', 'days'][i % 3];
  const words = WORDS[key] ?? WORDS[key.replace(/^(suggested|source|uploaded|created|actor|machine)/, '').replace(/^./, (c) => c.toLowerCase())];
  return words ? pick(words, i) : `${key} ${i + 1}`;
}

// ---------- contract validation ----------
function validateBody(schema, body, path = '') {
  schema = resolve(schema);
  const errors = [];
  if (!schema?.properties) return errors;
  if (body === null || typeof body !== 'object' || Array.isArray(body)) return [`${path || 'body'}: expected object`];
  for (const k of Object.keys(body)) if (!schema.properties[k]) errors.push(`${path}${k}: field not in contract`);
  for (const r of schema.required ?? []) if (body[r] === undefined || body[r] === null || body[r] === '') errors.push(`${path}${r}: required`);
  for (const [k, p] of Object.entries(schema.properties)) {
    const v = body[k];
    if (v === undefined || v === null) continue;
    const t = p.type;
    if (t === 'string' && typeof v !== 'string') errors.push(`${path}${k}: expected string`);
    if (t === 'integer' && !Number.isInteger(v)) errors.push(`${path}${k}: expected integer`);
    if (t === 'number' && typeof v !== 'number') errors.push(`${path}${k}: expected number`);
    if (t === 'boolean' && typeof v !== 'boolean') errors.push(`${path}${k}: expected boolean`);
    if (t === 'array' && !Array.isArray(v)) errors.push(`${path}${k}: expected array`);
    if (t === 'array' && Array.isArray(v) && p.items?.type === 'integer' && !v.every(Number.isInteger)) errors.push(`${path}${k}: expected integer[]`);
    if (t === 'string' && typeof v === 'string') {
      if (p.maxLength !== undefined && v.length > p.maxLength) errors.push(`${path}${k}: longer than ${p.maxLength}`);
      if (p.minLength && v.length < p.minLength) errors.push(`${path}${k}: shorter than ${p.minLength}`);
      if (p.pattern && !new RegExp(`^(?:${p.pattern})$`).test(v)) errors.push(`${path}${k}: pattern mismatch`);
      if (p.format === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(v)) errors.push(`${path}${k}: expected yyyy-mm-dd`);
    }
    if ((t === 'integer' || t === 'number') && typeof v === 'number') {
      if (p.minimum !== undefined && v < p.minimum) errors.push(`${path}${k}: below ${p.minimum}`);
      if (p.maximum !== undefined && v > p.maximum) errors.push(`${path}${k}: above ${p.maximum}`);
    }
  }
  return errors;
}

function validateQuery(op, url) {
  const errors = [];
  const declared = (op.parameters ?? []).filter((p) => p.in === 'query');
  for (const key of url.searchParams.keys()) if (!declared.some((p) => p.name === key)) errors.push(`query ${key}: not in contract`);
  for (const p of declared) {
    const v = url.searchParams.get(p.name);
    if (p.required && (v === null || v === '')) errors.push(`query ${p.name}: required`);
    if (v === null) continue;
    if (p.schema.type === 'integer' && !/^-?\d+$/.test(v)) errors.push(`query ${p.name}: expected integer, got "${v}"`);
    if (p.schema.type === 'boolean' && !/^(true|false)$/.test(v)) errors.push(`query ${p.name}: expected boolean`);
    if (p.schema.format === 'date' && !/^\d{4}-\d{2}-\d{2}$/.test(v)) errors.push(`query ${p.name}: expected date`);
  }
  return errors;
}

// ---------- server ----------
const violations = [];
const log = [];
const covered = new Set(); // operationIds served successfully since start (not cleared by reset)

function send(res, status, body, headers = {}) {
  const isText = typeof body === 'string';
  res.writeHead(status, { 'Content-Type': isText ? 'text/csv' : 'application/json', ...headers });
  res.end(isText ? body : body === undefined ? '' : JSON.stringify(body));
}

function readBody(req) {
  return new Promise((resolveBody) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolveBody(Buffer.concat(chunks)));
  });
}

createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${port}`);
  if (url.pathname === '/__mock/violations') return send(res, 200, violations);
  if (url.pathname === '/__mock/log') return send(res, 200, log);
  if (url.pathname === '/__mock/coverage') return send(res, 200, { covered: [...covered], total: ops.length });
  if (url.pathname === '/__mock/reset') {
    violations.length = 0;
    log.length = 0;
    return send(res, 200, { ok: true });
  }

  const match = ops.find((o) => o.method === req.method && o.regex.test(url.pathname));
  const raw = await readBody(req);
  const entry = { method: req.method, path: url.pathname, query: url.search, operationId: match?.op.operationId };
  log.push(entry);

  const fail = (status, problems) => {
    violations.push({ ...entry, problems });
    console.log(`✗ ${req.method} ${url.pathname}${url.search} → ${status}  ${problems.join('; ')}`);
    return send(res, status, { status, error: 'Contract violation', message: problems.join('; '), fieldErrors: {} });
  };

  if (!match) return fail(404, [`no operation for ${req.method} ${url.pathname}`]);
  const { op } = match;

  const auth = req.headers.authorization ?? '';
  // Tokens containing "expired" simulate an expired access token (exercises the client's refresh flow).
  if (!PUBLIC.has(url.pathname) && (!/^Bearer mock-accessToken-/.test(auth) || auth.includes('expired'))) {
    return send(res, 401, { status: 401, error: 'Unauthorized', message: 'Missing or invalid bearer token' });
  }

  const problems = validateQuery(op, url);
  const ctype = req.headers['content-type'] ?? '';
  if (op.requestBody) {
    if (url.pathname === '/api/imports/upload') {
      if (!ctype.startsWith('multipart/form-data') || !raw.includes('name="file"')) problems.push('expected multipart/form-data with a "file" part');
    } else if (raw.length === 0 && op.requestBody.required) {
      problems.push('request body required');
    } else if (raw.length) {
      let body;
      try {
        body = JSON.parse(raw.toString());
      } catch {
        problems.push('body is not valid JSON');
      }
      if (body !== undefined) problems.push(...validateBody(Object.values(op.requestBody.content)[0].schema, body));
    }
  } else if (raw.length) {
    problems.push('operation takes no request body');
  }
  if (problems.length) return fail(400, problems);

  covered.add(op.operationId);
  console.log(`✓ ${req.method} ${url.pathname}${url.search}  (${op.operationId})`);
  const [status, response] = Object.entries(op.responses)[0];
  const content = response.content && Object.values(response.content)[0];
  if (!content) return send(res, Number(status));
  if (op.operationId === 'exportRecords') {
    return send(res, 200, 'id,recordDate,machine,downtimeMinutes\n100,2025-09-01,Hydraulic Press 2,630\n', {
      'Content-Disposition': 'attachment; filename="records-export.csv"',
    });
  }
  let data = sample(content.schema, '', 0);
  if (op.operationId === 'getJob' || op.operationId === 'rerunJob' || op.operationId === 'getLatestJob') {
    data.errorMessage = null;
  }
  if (op.operationId === 'getRecord') data.rejectedReason = null;
  if (['getConversation', 'startConversation', 'sendMessage', 'requestEdit'].includes(op.operationId)) {
    data.resultingRecordId = null;
  }
  send(res, Number(status), data);
}).listen(port, () => console.log(`Contract mock server on http://localhost:${port} (${ops.length} operations)`));
