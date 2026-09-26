// Generates docs/API_INVENTORY.md from openapi.json so the inventory never drifts from the contract.
import { readFileSync, writeFileSync } from 'node:fs';

const spec = JSON.parse(readFileSync(new URL('../openapi.json', import.meta.url)));
const schemas = spec.components.schemas;
const refName = (ref) => ref.split('/').pop();

function typeOf(schema) {
  if (!schema) return '—';
  if (schema.$ref) return refName(schema.$ref);
  if (schema.type === 'array') return `${typeOf(schema.items)}[]`;
  if (schema.type === 'object' && schema.additionalProperties) return `map<string, ${typeOf(schema.additionalProperties)}>`;
  return schema.format ? `${schema.type}(${schema.format})` : schema.type;
}

function constraints(p) {
  const c = [];
  if (p.minLength) c.push(`min ${p.minLength}`);
  if (p.maxLength !== undefined) c.push(`max ${p.maxLength}`);
  if (p.minimum !== undefined) c.push(`≥ ${p.minimum}`);
  if (p.maximum !== undefined) c.push(`≤ ${p.maximum}`);
  if (p.pattern) c.push(`pattern \`${p.pattern}\``);
  if (p.default !== undefined) c.push(`default ${p.default}`);
  return c.join(', ');
}

const byTag = {};
for (const [path, ops] of Object.entries(spec.paths)) {
  for (const [method, op] of Object.entries(ops)) {
    (byTag[op.tags[0]] ??= []).push({ path, method: method.toUpperCase(), op });
  }
}

let out = `# API Inventory\n\n_Generated from \`openapi.json\` by \`scripts/generate-inventory.mjs\` — do not edit by hand._\n\n`;
out += `Security: every operation inherits the global \`bearerAuth\` requirement (HTTP bearer, JWT) — no operation declares an override. Request headers: \`Authorization: Bearer <accessToken>\` on all calls; \`X-Webhook-Token\` (optional) on \`inbound\` only. No operation documents error responses; only success codes are listed.\n\n`;

for (const [tag, list] of Object.entries(byTag).sort()) {
  out += `## ${tag}\n\n`;
  for (const { path, method, op } of list) {
    out += `### \`${op.operationId}\` — ${method} \`${path}\`\n\n`;
    const params = op.parameters ?? [];
    for (const kind of ['path', 'query', 'header']) {
      const ps = params.filter((p) => p.in === kind);
      if (!ps.length) continue;
      out += `- **${kind} params:** ` + ps.map((p) => `\`${p.name}\` ${typeOf(p.schema)}${p.required ? ' (required)' : ''}${constraints(p.schema) ? ` [${constraints(p.schema)}]` : ''}`).join('; ') + '\n';
    }
    if (params.some((p) => p.name === 'page')) out += `- **pagination:** \`page\`/\`size\` query params (0-based page)\n`;
    const body = op.requestBody?.content && Object.entries(op.requestBody.content)[0];
    if (body) out += `- **request body** (${body[0]}${op.requestBody.required ? ', required' : ''}): \`${typeOf(body[1].schema)}\`${body[1].schema.properties ? ` { ${Object.keys(body[1].schema.properties).join(', ')} }` : ''}\n`;
    for (const [code, r] of Object.entries(op.responses)) {
      const c = r.content && Object.values(r.content)[0];
      out += `- **response ${code}:** ${c ? `\`${typeOf(c.schema)}\`` : '_no body_'}\n`;
    }
    out += '\n';
  }
}

out += `## Schemas\n\n`;
for (const [name, s] of Object.entries(schemas).sort()) {
  out += `### ${name}\n\n| Field | Type | Required | Constraints |\n|---|---|---|---|\n`;
  for (const [f, p] of Object.entries(s.properties ?? {})) {
    out += `| \`${f}\` | ${typeOf(p)} | ${(s.required ?? []).includes(f) ? 'yes' : ''} | ${constraints(p)} |\n`;
  }
  out += '\n';
}

writeFileSync(new URL('../docs/API_INVENTORY.md', import.meta.url), out);
console.log('wrote docs/API_INVENTORY.md');
