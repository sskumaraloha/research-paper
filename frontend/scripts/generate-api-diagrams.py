#!/usr/bin/env python3
"""Render frontend/openapi.json as a small set of SVG reference sheets.

Each endpoint card shows method, path, parameters, request body and response,
with field types and validation constraints taken straight from the spec.
Schemas used by more than one card on a sheet (or nested inside another
schema) are drawn once in that sheet's "Types" section instead of repeating.

Usage: python3 frontend/scripts/generate-api-diagrams.py [openapi.json] [out_dir]
"""
import json
import os
import sys
from collections import Counter, OrderedDict
from xml.sax.saxutils import escape

HERE = os.path.dirname(os.path.abspath(__file__))
SPEC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, '..', 'openapi.json')
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', 'docs', 'api-diagrams')

spec = json.load(open(SPEC))
SCHEMAS = spec['components']['schemas']
METHODS = ('get', 'post', 'put', 'patch', 'delete')

# ---------------------------------------------------------------- sheets
SHEETS = [
    ('01-auth-users-plants', 'Auth, users, plants, config & platform',
     ['auth-controller', 'user-controller', 'plant-controller', 'config-controller',
      'platform-admin-controller']),
    ('02-machines-records-parts-search', 'Machines, maintenance records, spare parts & search',
     ['machine-controller', 'maintenance-record-controller', 'spare-part-controller',
      'search-controller']),
    ('03-imports-validation-entry-schedules', 'Data intake: imports, validation, guided entry, WhatsApp & schedules',
     ['import-controller', 'validation-controller', 'entry-agent-controller',
      'whats-app-webhook-controller', 'schedule-controller']),
    ('04-dashboard-analytics-insights-assistant', 'Dashboard, analytics, insights, assistant, notifications & audit',
     ['dashboard-controller', 'analytics-controller', 'insight-controller',
      'assistant-controller', 'notification-controller', 'audit-controller']),
]

SHORT = ['Auth, users, plants, config, platform', 'Machines, records, parts, search',
         'Imports, validation, entry, WhatsApp, schedules',
         'Dashboard, analytics, insights, assistant, notifications, audit']

# ---------------------------------------------------------------- palette
C = dict(
    bg='#f6f7f9', card='#ffffff', border='#d9dde3', text='#1d2430', muted='#5f6b7a',
    faint='#8a95a3', req='#c2410c', name='#1d2430', type='#4338ca', ref='#0f766e',
    cons='#8a5a00', section='#eef1f5', title='#111827', accent='#2f5bd3',
    typecard='#fbfaf5', typeborder='#e2dcc5',
)
METHOD_COLOR = {'GET': '#15803d', 'POST': '#2563eb', 'PUT': '#c2410c',
                'PATCH': '#7c3aed', 'DELETE': '#b91c1c'}
SANS = "Inter, 'Segoe UI', Helvetica, Arial, sans-serif"
MONO = "'JetBrains Mono', 'SF Mono', Menlo, Consolas, 'DejaVu Sans Mono', monospace"
MONO_W = 0.61  # average glyph width / font size for the monospace stack

# ---------------------------------------------------------------- helpers
def ref_name(s):
    return s['$ref'].split('/')[-1] if s and '$ref' in s else None


def page_item(name):
    """PageResponseFooResponse -> FooResponse (spring Page envelope)."""
    if name and name.startswith('PageResponse') and name in SCHEMAS:
        return ref_name(SCHEMAS[name]['properties']['content']['items'])
    return None


def type_str(s):
    if not s:
        return 'empty'
    r = ref_name(s)
    if r:
        return f'Page<{page_item(r)}>' if page_item(r) else r
    t = s.get('type')
    if t == 'array':
        return type_str(s['items']) + '[]'
    if t == 'object' and 'additionalProperties' in s:
        return f'map<string, {type_str(s["additionalProperties"])}>'
    if t == 'object':
        return 'object'
    fmt = s.get('format')
    if t == 'integer':
        return fmt or 'integer'
    if t == 'number':
        return fmt or 'number'
    if t == 'string' and fmt:
        return {'date': 'date', 'date-time': 'date-time', 'binary': 'file'}.get(fmt, fmt)
    return {'boolean': 'boolean'}.get(t, t or 'any')


def constraints(s):
    out = []
    lo, hi = s.get('minLength'), s.get('maxLength')
    if hi is not None:
        out.append(f'len {lo}–{hi}' if lo else f'max {hi}')
    elif lo:
        out.append(f'min len {lo}')
    if 'minimum' in s or 'maximum' in s:
        out.append(f'{s.get("minimum", "")}..{s.get("maximum", "")}')
    if 'pattern' in s:
        out.append('/' + s['pattern'] + '/')
    if 'enum' in s:
        out.append(' | '.join(s['enum']))
    return ', '.join(out)


def refs_in(s, acc):
    """Collect schema names referenced anywhere inside s (excluding Page envelopes)."""
    if not isinstance(s, dict):
        return
    r = ref_name(s)
    if r:
        acc.append(page_item(r) or r)
        return
    for k in ('items', 'additionalProperties'):
        if isinstance(s.get(k), dict):
            refs_in(s[k], acc)
    for v in s.get('properties', {}).values():
        refs_in(v, acc)


def primary(s):
    """The schema a request/response is 'about': unwraps arrays and Page<T>."""
    if not s:
        return None
    if s.get('type') == 'array':
        return primary(s['items'])
    r = ref_name(s)
    return (page_item(r) or r) if r else None


def fields_of(name):
    s = SCHEMAS[name]
    req = set(s.get('required', []))
    rows = []
    for k, v in s.get('properties', {}).items():
        rows.append((k, k in req, type_str(v), constraints(v), ref_name(v) or primary(v)))
    return rows


def inline_fields(s):
    """Fields for an inline (non-$ref) object schema, e.g. the upload body."""
    req = set(s.get('required', []))
    return [(k, k in req, type_str(v), constraints(v), None) for k, v in s.get('properties', {}).items()]


# ---------------------------------------------------------------- collect operations
ops = []
for path, item in spec['paths'].items():
    for m, o in item.items():
        if m not in METHODS:
            continue
        rb = o.get('requestBody')
        body = None
        if rb:
            ct, media = next(iter(rb['content'].items()))
            body = media.get('schema')
            if o['operationId'] == 'uploadFile':
                ct = 'multipart/form-data'  # see docs/API_ANALYSIS.md Q3
            body = (ct, body)
        code, resp = next(iter(o['responses'].items()))
        rs = next(iter(resp.get('content', {}).values()), {}).get('schema')
        ops.append(dict(method=m.upper(), path=path, id=o['operationId'], tag=o['tags'][0],
                        params=o.get('parameters', []), body=body, code=code,
                        status=resp.get('description', ''), resp=rs))

ORDER = {'GET': 0, 'POST': 1, 'PUT': 2, 'PATCH': 3, 'DELETE': 4}


def sort_key(op):
    return (op['path'].replace('{', '~'), ORDER[op['method']])


# ---------------------------------------------------------------- svg primitives
class Svg:
    def __init__(self):
        self.parts = []

    def add(self, s):
        self.parts.append(s)

    def rect(self, x, y, w, h, fill, stroke=None, rx=8, sw=1):
        st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ''
        self.add(f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{rx}" fill="{fill}"{st}/>')

    def text(self, x, y, s, size=13, fill=None, weight=400, font=MONO, anchor='start'):
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-family="{font}" font-size="{size}" '
                 f'font-weight="{weight}" fill="{fill or C["text"]}" text-anchor="{anchor}">{escape(str(s))}</text>')

    def spans(self, x, y, pieces, size=13, font=MONO):
        """pieces: list of (text, fill, weight)."""
        inner = ''.join(f'<tspan fill="{f}" font-weight="{w}">{escape(t)}</tspan>' for t, f, w in pieces)
        self.add(f'<text x="{x:.1f}" y="{y:.1f}" font-family="{font}" font-size="{size}" xml:space="preserve">{inner}</text>')


def mono_w(n, size=13):
    return n * size * MONO_W


# ---------------------------------------------------------------- card model
LH = 19          # field line height
FS = 13          # field font size
PAD = 16
CARD_W = 600


def wrap(s, n):
    out = []
    while len(s) > n:
        cut = s.rfind(' ', 0, n)
        cut = cut if cut > n // 2 else n
        out.append(s[:cut])
        s = s[cut:].lstrip()
    out.append(s)
    return out


def field_block(rows, width):
    """Lay out field rows as name | type | constraints; returns list of line piece-lists."""
    if not rows:
        return []
    name_w = max(len(r[0]) + (1 if r[1] else 0) for r in rows)
    name_w = min(name_w, 26)
    type_w = min(max(len(r[2]) for r in rows), 30)
    max_chars = int(width / (FS * MONO_W))
    lines = []
    for name, req, typ, cons, ref in rows:
        label = name + ('*' if req else '')
        pieces = [(name, C['name'], 500)]
        if req:
            pieces.append(('*', C['req'], 700))
        pieces.append((' ' * max(1, name_w - len(label) + 2), C['text'], 400))
        pieces.append((typ, C['ref'] if ref else C['type'], 500 if ref else 400))
        used = max(name_w, len(label)) + 2 + len(typ)
        if cons:
            pad = max(2, type_w - len(typ) + 2)
            room = max_chars - used - pad
            if room >= len(cons):
                pieces.append((' ' * pad + cons, C['cons'], 400))
                lines.append(pieces)
            else:
                lines.append(pieces)
                for chunk in wrap(cons, max_chars - name_w - 4):
                    lines.append([(' ' * (name_w + 2) + chunk, C['cons'], 400)])
        else:
            lines.append(pieces)
    return lines


class Card:
    """A box with a header and labelled sections of monospace lines."""

    def __init__(self, kind):
        self.kind = kind          # 'op' or 'type'
        self.sections = []        # (label, sublabel, lines)
        self.header = None

    def height(self):
        h = 0
        if self.kind == 'op':
            h += 62
        else:
            h += 40
        for label, sub, lines in self.sections:
            h += (26 if label else 4) + max(1, len(lines)) * LH + 6
        return h + 10

    def draw(self, svg, x, y):
        h = self.height()
        w = CARD_W
        if self.kind == 'op':
            svg.rect(x, y, w, h, C['card'], C['border'], rx=10)
            m, path, opid, tag, code, status = self.header
            mc = METHOD_COLOR[m]
            svg.rect(x, y, 6, h, mc, rx=3)
            pw = 62
            svg.rect(x + PAD, y + 13, pw, 24, mc, rx=5)
            svg.text(x + PAD + pw / 2, y + 30, m, size=13, fill='#fff', weight=700, font=SANS, anchor='middle')
            size = 15 if mono_w(len(path), 15) < w - pw - 3 * PAD else 13
            svg.spans(x + PAD + pw + 12, y + 31, path_pieces(path), size=size)
            svg.spans(x + PAD, y + 53, [(opid + '()', C['muted'], 500),
                                        ('   ' + tag.replace('-controller', ''), C['faint'], 400)],
                      size=12, font=MONO)
            svg.text(x + w - PAD, y + 53, f'{code} {status}', size=12, fill=C['muted'], weight=600,
                     font=SANS, anchor='end')
            cy = y + 62
        else:
            svg.rect(x, y, w, h, C['typecard'], C['typeborder'], rx=10)
            name, note = self.header
            svg.text(x + PAD, y + 26, name, size=15, fill=C['ref'], weight=700)
            if note:
                svg.text(x + w - PAD, y + 26, note, size=12, fill=C['faint'], font=SANS, anchor='end')
            cy = y + 40
        for label, sub, lines in self.sections:
            if label:
                svg.rect(x + 8 + (6 if self.kind == 'op' else 0), cy + 2, w - 16 - (6 if self.kind == 'op' else 0),
                         20, C['section'] if self.kind == 'op' else '#f2eedf', rx=4)
                svg.spans(x + PAD, cy + 16, [(label, C['accent'], 700)] +
                          ([('  ' + sub, C['muted'], 500)] if sub else []), size=11, font=SANS)
                cy += 26
            else:
                cy += 4
            if not lines:
                svg.text(x + PAD, cy + 14, '—', size=FS, fill=C['faint'])
                cy += LH
            for ln in lines:
                svg.spans(x + PAD, cy + 14, ln, size=FS)
                cy += LH
            cy += 6


def path_pieces(path):
    out = []
    for seg in path.split('/')[1:]:
        out.append(('/', C['faint'], 400))
        out.append((seg, C['req'] if seg.startswith('{') else C['title'], 700 if seg.startswith('{') else 600))
    return out


INNER_W = CARD_W - 2 * PAD


def note_lines(text):
    return [[(ln, C['muted'], 400)] for ln in wrap(text, int(INNER_W / (FS * MONO_W)))]


def op_card(op, shared):
    card = Card('op')
    card.header = (op['method'], op['path'], op['id'], op['tag'], op['code'], op['status'])
    # parameters
    prow = []
    for p in op['params']:
        where = {'path': 'path', 'query': 'query', 'header': 'header'}[p['in']]
        prow.append((p['name'], p.get('required', False), type_str(p['schema']), where, None))
    if prow:
        card.sections.append(('PARAMS', 'path · query · header', field_block(prow, INNER_W)))
    # request body
    if op['body']:
        ct, s = op['body']
        r = ref_name(s)
        if r and r not in shared:
            card.sections.append(('REQUEST BODY', f'{r}  ·  {ct}', field_block(fields_of(r), INNER_W)))
        elif r:
            card.sections.append(('REQUEST BODY', f'{ct}', [[(r, C['ref'], 600), ('   → see Types below', C['faint'], 400)]]))
        else:
            card.sections.append(('REQUEST BODY', ct, field_block(inline_fields(s), INNER_W)))
    # response
    s = op['resp']
    label = f'RESPONSE {op["code"]}'
    if s is None:
        card.sections.append((label, 'no body', note_lines('Empty body.')))
    else:
        ts = type_str(s)
        p = primary(s)
        if p and p not in shared:
            lines = []
            if ts != p:
                lines.append([(ts, C['ref'], 600), (page_note(s), C['faint'], 400)])
                lines.append([('', C['text'], 400)])
                lines.append([(f'{p}', C['ref'], 700)])
            card.sections.append((label, ts if ts == p else '', lines + field_block(fields_of(p), INNER_W)))
        elif p:
            card.sections.append((label, '', [[(ts, C['ref'], 600), ((page_note(s) + ' · item in Types') if page_note(s) else '   → see Types below', C['faint'], 400)]]))
        else:
            extra = {'exportRecords': 'Raw text body, saved as a file (format not documented; likely CSV).',
                     'getDictionary': 'Nested lookup: { group: { key: [values…] } }.',
                     'suggestions': 'List of suggested question strings.'}.get(op['id'], '')
            card.sections.append((label, '', [[(ts, C['type'], 600)]] + (note_lines(extra) if extra else [])))
    if op['id'] == 'uploadFile':
        card.sections.append(('NOTE', '', note_lines('Spec says application/json, but a file must be sent as '
                                                      'multipart/form-data with a part named "file".')))
    if op['id'] == 'inbound':
        card.sections.append(('NOTE', '', note_lines('Server-to-server webhook (WhatsApp gateway). '
                                                      'Not called by the web frontend.')))
    return card


def page_note(s):
    r = ref_name(s)
    if r and page_item(r):
        return '   paged: content[] + meta'
    if s.get('type') == 'array':
        return '   JSON array'
    return ''


def type_card(name, used_by):
    card = Card('type')
    card.header = (name, used_by)
    card.sections.append((None, None, field_block(fields_of(name), INNER_W)))
    return card


# ---------------------------------------------------------------- layout
COLS = 4
GAP = 22
MARGIN = 36
WIDTH = MARGIN * 2 + COLS * CARD_W + (COLS - 1) * GAP


def masonry(cards, top):
    heights = [top] * COLS
    placed = []
    for c in cards:
        i = heights.index(min(heights))
        x = MARGIN + i * (CARD_W + GAP)
        placed.append((c, x, heights[i]))
        heights[i] += c.height() + GAP
    return placed, max(heights)


def header(svg, title, subtitle, sheet_no, total):
    svg.text(MARGIN, 58, 'Maintenance Intelligence Platform API', size=30, weight=800, font=SANS, fill=C['title'])
    svg.text(MARGIN, 92, title, size=21, weight=600, font=SANS, fill=C['accent'])
    svg.text(MARGIN, 120, subtitle, size=14, font=SANS, fill=C['muted'])
    label = f'Sheet {sheet_no} of {total}' if sheet_no else f'Overview · detail on sheets 1–{total}'
    svg.text(WIDTH - MARGIN, 58, label, size=16, weight=700, font=SANS,
             fill=C['muted'], anchor='end')
    svg.text(WIDTH - MARGIN, 82, f'{spec["info"]["title"]} · {spec["info"]["version"]} · source: frontend/openapi.json',
             size=12, font=SANS, fill=C['faint'], anchor='end')
    legend(svg, WIDTH - MARGIN, 108)


def legend(svg, right, y):
    items = [('GET', None), ('POST', None), ('PUT', None)]
    x = right - 930
    for m, _ in items:
        svg.rect(x, y - 14, 50, 20, METHOD_COLOR[m], rx=4)
        svg.text(x + 25, y + 1, m, size=11, fill='#fff', weight=700, font=SANS, anchor='middle')
        x += 58
    svg.spans(x + 6, y + 1, [('name', C['name'], 600), ('*', C['req'], 700), (' required  ', C['muted'], 400),
                              ('int64', C['type'], 400), (' scalar  ', C['muted'], 400),
                              ('Schema', C['ref'], 600), (' object/ref  ', C['muted'], 400),
                              ('max 100', C['cons'], 400), (' validation  ', C['muted'], 400),
                              ('{id}', C['req'], 700), (' path var', C['muted'], 400)], size=12)


def finish(svg, height, name):
    body = '\n'.join(svg.parts)
    out = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{height:.0f}" '
           f'viewBox="0 0 {WIDTH} {height:.0f}">\n'
           f'<rect width="100%" height="100%" fill="{C["bg"]}"/>\n{body}\n</svg>\n')
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, name + '.svg'), 'w') as f:
        f.write(out)
    return out


def section_title(svg, y, title, sub):
    svg.add(f'<line x1="{MARGIN}" y1="{y:.1f}" x2="{WIDTH - MARGIN}" y2="{y:.1f}" stroke="{C["border"]}" stroke-width="1.5"/>')
    svg.text(MARGIN, y + 34, title, size=20, weight=700, font=SANS, fill=C['title'])
    svg.text(MARGIN + len(title) * 11.5 + 16, y + 34, sub, size=14, font=SANS, fill=C['muted'])


# ---------------------------------------------------------------- domain sheets
TOTAL = len(SHEETS)


def render_sheet(no, key, title, tags):
    sheet_ops = sorted([o for o in ops if o['tag'] in tags], key=lambda o: (tags.index(o['tag']), sort_key(o)))
    # schemas referenced as the primary body/response of each card
    prim = Counter()
    nested = set()
    for o in sheet_ops:
        for s in ([o['body'][1]] if o['body'] else []) + ([o['resp']] if o['resp'] else []):
            p = primary(s)
            if p:
                prim[p] += 1
    shared = {n for n, c in prim.items() if c > 1}
    # nested refs of everything expanded on the sheet (transitively)
    todo = list(prim)
    seen = set()
    while todo:
        n = todo.pop()
        if n in seen:
            continue
        seen.add(n)
        acc = []
        for v in SCHEMAS[n].get('properties', {}).values():
            refs_in(v, acc)
        for a in acc:
            nested.add(a)
            todo.append(a)
    shared |= nested

    svg = Svg()
    counts = Counter(o['method'] for o in sheet_ops)
    sub = (f'{len(sheet_ops)} endpoints  ·  ' + '  '.join(f'{m} {counts[m]}' for m in ('GET', 'POST', 'PUT') if counts[m])
           + f'  ·  controllers: ' + ', '.join(t.replace('-controller', '') for t in tags)
           + '  ·  all calls need  Authorization: Bearer <accessToken>  (except login/register/refresh/password reset)')
    header(svg, title, sub, no, TOTAL)

    top = 150
    y = top
    for tag in tags:
        group = [o for o in sheet_ops if o['tag'] == tag]
        section_title(svg, y, tag.replace('-controller', '').replace('-', ' ').title(),
                      f'{len(group)} endpoint{"s" if len(group) > 1 else ""}  ·  ' +
                      ', '.join(sorted({"/".join(o["path"].split("/")[:3]) for o in group})))
        placed, bottom = masonry([op_card(o, shared) for o in group], y + 52)
        for c, x, cy in placed:
            c.draw(svg, x, cy)
        y = bottom + 8

    types = sorted(shared)
    if types:
        users = {}
        for o in sheet_ops:
            for s in ([o['body'][1]] if o['body'] else []) + ([o['resp']] if o['resp'] else []):
                p = primary(s)
                if p in shared:
                    users.setdefault(p, []).append(o['id'])
        section_title(svg, y, 'Types', f'{len(types)} schemas referenced by more than one endpoint above, '
                                       'or nested inside another schema')
        cards = []
        for t in types:
            u = users.get(t)
            note = ('used by ' + ', '.join(u)) if u and len(', '.join(u)) < 60 else (
                f'used by {len(u)} endpoints' if u else 'nested type')
            cards.append(type_card(t, note))
        placed, y = masonry(cards, y + 52)
        for c, x, cy in placed:
            c.draw(svg, x, cy)
    finish(svg, y + MARGIN, key)
    return sheet_ops


# ---------------------------------------------------------------- overview sheet
def render_overview(all_sheets):
    svg = Svg()
    counts = Counter(o['method'] for o in ops)
    header(svg, 'Overview: endpoint map, conventions & request lifecycle',
           f'{len(ops)} operations  ·  {len(spec["paths"])} paths  ·  {len({o["tag"] for o in ops})} controllers  ·  '
           f'{len(SCHEMAS)} schemas  ·  ' + '  '.join(f'{m} {counts[m]}' for m in ('GET', 'POST', 'PUT')),
           0, TOTAL)
    y0 = 160

    # --- stat tiles
    tiles = [(str(len(ops)), 'endpoints'), (str(len(spec['paths'])), 'URL paths'),
             (str(len({o['tag'] for o in ops})), 'controllers'), (str(len(SCHEMAS)), 'schemas (DTOs)'),
             (str(counts['GET']), 'GET'), (str(counts['POST']), 'POST'), (str(counts['PUT']), 'PUT')]
    tw = (WIDTH - 2 * MARGIN - (len(tiles) - 1) * 16) / len(tiles)
    for i, (v, l) in enumerate(tiles):
        x = MARGIN + i * (tw + 16)
        svg.rect(x, y0, tw, 96, C['card'], C['border'], rx=10)
        col = METHOD_COLOR.get(l, C['title'])
        svg.text(x + 22, y0 + 56, v, size=40, weight=800, font=SANS, fill=col)
        svg.text(x + 22, y0 + 82, l, size=15, weight=500, font=SANS, fill=C['muted'])
    y = y0 + 96 + 40

    # --- endpoint map: one column per sheet, rows per controller with method chips
    section_title(svg, y, 'Endpoint map', 'every endpoint by sheet and controller; numbers in brackets are endpoint counts')
    y += 56
    colw = (WIDTH - 2 * MARGIN - (len(SHEETS) - 1) * GAP) / len(SHEETS)
    maxy = y
    for i, ((key, title, tags), sheet_ops) in enumerate(zip(SHEETS, all_sheets)):
        x = MARGIN + i * (colw + GAP)
        cy = y
        lines = 0
        for t in tags:
            lines += 1.6 + sum(1 for o in sheet_ops if o['tag'] == t)
        h = 70 + lines * 21 + 10
        svg.rect(x, cy, colw, h, C['card'], C['border'], rx=10)
        svg.text(x + 18, cy + 30, f'Sheet {i + 1}', size=13, weight=700, font=SANS, fill=C['accent'])
        svg.text(x + 18, cy + 52, SHORT[i], size=15, weight=700, font=SANS,
                 fill=C['title'])
        cy += 70
        for t in tags:
            g = [o for o in sheet_ops if o['tag'] == t]
            svg.text(x + 18, cy + 18, f'{t.replace("-controller", "")}  ({len(g)})', size=14, weight=700,
                     font=SANS, fill=C['title'])
            cy += 30
            for o in g:
                mc = METHOD_COLOR[o['method']]
                svg.rect(x + 18, cy + 3, 44, 16, mc, rx=3)
                svg.text(x + 40, cy + 15, o['method'], size=10, fill='#fff', weight=700, font=SANS, anchor='middle')
                svg.spans(x + 72, cy + 16, path_pieces(o['path']), size=12.5)
                cy += 21
            cy += 4
        maxy = max(maxy, y + h)
    y = maxy + 40

    # --- conventions + auth flow
    section_title(svg, y, 'Conventions every call follows', 'from openapi.json and frontend/docs/API_ANALYSIS.md')
    y += 56
    conv = Card('type')
    conv.header = ('Request conventions', '')
    rows = [
        ('Base URL', 'http://localhost:8080 (dev: Vite proxies /api → VITE_PROXY_TARGET)'),
        ('Auth', 'Authorization: Bearer <accessToken>  (JWT, bearerAuth)'),
        ('Public', 'login, register, demo-login, refresh, forgot/reset-password, reset-password/validate'),
        ('Content', 'JSON request bodies; responses are JSON (export returns text)'),
        ('Upload', 'POST /api/imports/upload → multipart/form-data, part "file"'),
        ('IDs', 'int64 numbers; path vars like {machineId}'),
        ('Dates', 'date = "YYYY-MM-DD", date-time = ISO-8601'),
        ('Plant scope', 'most list/analytics calls require ?plantId=<id>'),
        ('Paging', '?page=0&size=20 → Page<T> envelope (right)'),
        ('Errors', 'no error schema documented; read body.message, fieldErrors or errors[] defensively'),
        ('Enums', 'role/status/criticality/severity are plain strings; roles from GET /api/users/roles'),
    ]
    lines = []
    for k, v in rows:
        first = True
        for chunk in wrap(v, 52):
            lines.append([((k if first else '').ljust(13), C['name'], 700), (chunk, C['text'], 400)])
            first = False
    conv.sections.append((None, None, lines))

    page = Card('type')
    page.header = ('Page<T>  paginated envelope', '')
    page.sections.append((None, None, field_block([
        ('content', False, 'T[]', 'the rows', 'x'), ('page', False, 'int32', '0-based page index', None),
        ('size', False, 'int32', 'page size', None), ('totalElements', False, 'int64', '', None),
        ('totalPages', False, 'int32', '', None), ('first', False, 'boolean', '', None),
        ('last', False, 'boolean', '', None)], INNER_W)))
    page.sections.append(('QUERY', 'on paged endpoints', field_block([
        ('page', False, 'int32', 'optional', None), ('size', False, 'int32', 'optional', None)], INNER_W)))
    page.sections.append(('USED BY', '', note_lines('records, machine timeline, machines, parts, notifications, '
                                                    'audit, validation queue (items)')))

    auth = Card('type')
    auth.header = ('AuthResponse  (login / register / refresh / demo-login)', '')
    auth.sections.append((None, None, field_block(fields_of('AuthResponse'), INNER_W)))
    auth.sections.append(('user: UserProfileResponse', '', field_block(fields_of('UserProfileResponse'), INNER_W)))

    common = Card('type')
    common.header = ('Small shared shapes', '')
    for n in ('CountResponse', 'SimpleMessageResponse', 'NamedRef'):
        common.sections.append((n, '', field_block(fields_of(n), INNER_W)))

    placed, bottom = masonry([conv, page, auth, common], y)
    for c, x, cy in placed:
        c.draw(svg, x, cy)
    y = bottom + 20

    # --- flows
    section_title(svg, y, 'Lifecycles', 'how the calls chain together')
    y += 60
    y = flow(svg, y, 'Session', [
        ('POST /api/auth/login', '{ email, password }'),
        ('AuthResponse', 'accessToken, refreshToken, user'),
        ('GET /api/auth/me · /api/plants', 'profile + plants the user can see'),
        ('any API + Bearer', 'pass ?plantId for plant-scoped calls'),
        ('401 → POST /api/auth/refresh', '{ refreshToken } → new tokens, retry once'),
        ('POST /api/auth/logout', '{ refreshToken }'),
    ])
    y = flow(svg, y + 24, 'Record', [
        ('POST /api/imports/upload', 'file + ?plantId → ImportSummaryResponse'),
        ('GET /api/imports/{jobId}', 'status, counts, steps[]'),
        ('rows: auto-imported', 'or needs validation / rejected / invalid'),
        ('GET /api/validation/queue', 'Page<ValidationItemResponse>'),
        ('approve · edit-approve · reject', '→ ValidationDecisionResponse.recordId'),
        ('GET /api/records/{recordId}', 'RecordDetailResponse'),
    ])
    y = flow(svg, y + 24, 'Other ways in', [
        ('POST /api/records', 'manual form → RecordDetailResponse'),
        ('POST /api/entry/conversations', 'chat-guided entry, draft.missingFields'),
        ('…/messages  …/confirm', '→ resultingRecordId'),
        ('POST /api/webhooks/whatsapp', 'server-to-server, same chat flow'),
        ('POST /api/schedules/{id}/complete', 'preventive task done'),
        ('GET /api/insights · /analytics/*', 'read the derived data'),
    ])
    finish(svg, y + MARGIN, '00-overview')


def flow(svg, y, label, steps):
    n = len(steps)
    lw = 150
    bw = (WIDTH - 2 * MARGIN - lw - (n - 1) * 34) / n
    svg.text(MARGIN, y + 46, label, size=17, weight=700, font=SANS, fill=C['title'])
    for i, (a, b) in enumerate(steps):
        x = MARGIN + lw + i * (bw + 34)
        svg.rect(x, y, bw, 76, C['card'], C['border'], rx=10)
        svg.text(x + 16, y + 32, a, size=13.5, weight=700, fill=C['accent'])
        svg.text(x + 16, y + 56, b, size=13, font=SANS, fill=C['muted'])
        if i < n - 1:
            ax = x + bw + 6
            svg.add(f'<path d="M{ax:.1f} {y + 38} h20 m-7 -6 l7 6 l-7 6" fill="none" stroke="{C["faint"]}" '
                    f'stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>')
    return y + 76


if __name__ == '__main__':
    done = [render_sheet(i + 1, *s) for i, s in enumerate(SHEETS)]
    covered = sum(len(d) for d in done)
    assert covered == len(ops), f'{covered} of {len(ops)} operations placed on a sheet'
    render_overview(done)
    print(f'{covered} operations across {len(SHEETS)} sheets + overview → {os.path.relpath(OUT)}')
