// Statically verifies every API call in src/ against openapi.json:
//  - the METHOD + path exists as an operation
//  - every query param sent is documented for that operation, and every required query param is sent
// and reports operations that no frontend code calls.
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = new URL('..', import.meta.url).pathname;
const spec = JSON.parse(readFileSync(join(root, 'openapi.json'), 'utf8'));

const ops = [];
for (const [path, methods] of Object.entries(spec.paths)) {
  for (const [method, op] of Object.entries(methods)) {
    const pattern = new RegExp('^' + path.replace(/\{[^}]+\}/g, '[^/]+') + '$');
    const query = (op.parameters ?? []).filter((p) => p.in === 'query');
    ops.push({ path, method: method.toUpperCase(), op, pattern, query, used: false });
  }
}

// Like Spring MVC, prefer literal segments over path variables (/api/records/export before /api/records/{recordId}).
ops.sort((a, b) => (a.path.match(/\{/g) ?? []).length - (b.path.match(/\{/g) ?? []).length);

function walk(dir) {
  return readdirSync(dir).flatMap((f) => {
    const p = join(dir, f);
    return statSync(p).isDirectory() ? walk(p) : /\.(jsx?|mjs)$/.test(f) ? [p] : [];
  });
}

// Matches   method: 'GET', url: '/api/x'   or   url: `/api/x/${id}`   with optional params: { a, b }
const CALL = /method:\s*'(\w+)',\s*url:\s*[`'"]([^`'"]+)[`'"](?:,\s*params:\s*\{([^}]*)\})?/g;
// Also match the raw refresh call in client.js: axios.post(`${baseURL}/api/auth/refresh`
const RAW = /axios\.(get|post|put|delete)\(\s*`\$\{baseURL\}([^`]+)`/g;

const problems = [];
let calls = 0;
for (const file of walk(join(root, 'src'))) {
  const src = readFileSync(file, 'utf8');
  const found = [];
  for (const m of src.matchAll(CALL)) found.push({ method: m[1].toUpperCase(), url: m[2], params: m[3] });
  for (const m of src.matchAll(RAW)) found.push({ method: m[1].toUpperCase(), url: m[2] });
  for (const call of found) {
    calls++;
    const concrete = call.url.replace(/\$\{[^}]+\}/g, 'X');
    const where = `${relative(root, file)}: ${call.method} ${call.url}`;
    const match = ops.find((o) => o.method === call.method && o.pattern.test(concrete));
    if (!match) {
      problems.push(`UNDOCUMENTED  ${where}`);
      continue;
    }
    match.used = true;
    if (call.params !== undefined) {
      const sent = call.params.split(',').map((s) => s.trim().split(':')[0].trim()).filter(Boolean);
      const allowed = match.query.map((q) => q.name);
      for (const s of sent) if (!allowed.includes(s)) problems.push(`UNKNOWN PARAM '${s}'  ${where}`);
      for (const q of match.query) if (q.required && !sent.includes(q.name)) problems.push(`MISSING REQUIRED PARAM '${q.name}'  ${where}`);
    } else {
      for (const q of match.query) if (q.required) problems.push(`MISSING REQUIRED PARAM '${q.name}'  ${where}`);
    }
  }
}

// Every service export must be imported by some UI module; otherwise the operation is wrapped but never used.
const files = walk(join(root, 'src'));
const unwired = [];
for (const file of files.filter((f) => f.includes('/services/'))) {
  const src = readFileSync(file, 'utf8');
  for (const [, fn] of src.matchAll(/export const (\w+)/g)) {
    const importRe = new RegExp(`import\\s*\\{[^}]*\\b${fn}\\b[^}]*\\}\\s*from\\s*'[^']*services/`);
    const importAll = /import \* as \w+ from '[^']*authService'/;
    const used = files.some((f) => f !== file && (importRe.test(readFileSync(f, 'utf8')) || (file.endsWith('authService.js') && importAll.test(readFileSync(f, 'utf8')) && readFileSync(f, 'utf8').includes(`.${fn}(`))));
    if (!used) unwired.push(`${relative(root, file)}: ${fn}`);
  }
}
if (unwired.length) {
  for (const u of unwired) problems.push(`SERVICE NOT USED BY ANY PAGE  ${u}`);
}

const unused = ops.filter((o) => !o.used);
console.log(`Checked ${calls} API calls against ${ops.length} documented operations.`);
console.log(`Covered operations: ${ops.length - unused.length}/${ops.length}`);
if (unused.length) {
  console.log('Operations not called by the frontend:');
  for (const o of unused) console.log(`  - ${o.op.operationId}  ${o.method} ${o.path}`);
}
if (problems.length) {
  console.error('\nContract violations:');
  for (const p of problems) console.error('  ✗ ' + p);
  process.exit(1);
}
console.log('\n✓ Every API call matches openapi.json (method, path, query params).');
