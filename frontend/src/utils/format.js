const dateFmt = new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
const dateTimeFmt = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });
const numFmt = new Intl.NumberFormat();

/** API `format: date` values are ISO yyyy-mm-dd; parse as local dates to avoid timezone shifts. */
export function formatDate(value) {
  if (!value) return '—';
  const [y, m, d] = String(value).split('-').map(Number);
  if (!y || !m || !d) return String(value);
  return dateFmt.format(new Date(y, m - 1, d));
}

export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : dateTimeFmt.format(date);
}

export const formatNumber = (value, digits) =>
  value === null || value === undefined
    ? '—'
    : digits === undefined
      ? numFmt.format(value)
      : new Intl.NumberFormat(undefined, { maximumFractionDigits: digits, minimumFractionDigits: 0 }).format(value);

export function formatMinutes(minutes) {
  if (minutes === null || minutes === undefined) return '—';
  if (minutes < 60) return `${numFmt.format(minutes)} min`;
  const h = Math.floor(minutes / 60);
  const m = Math.round(minutes % 60);
  return m ? `${numFmt.format(h)}h ${m}m` : `${numFmt.format(h)}h`;
}

export const formatPct = (value, digits = 1) =>
  value === null || value === undefined ? '—' : `${formatNumber(value, digits)}%`;

/** Confidence values (0–1 doubles, per UpdatePlantSettingsRequest thresholds) shown as percentages. */
export const formatConfidence = (value) =>
  value === null || value === undefined ? '—' : `${Math.round(value * 100)}%`;

export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let n = bytes;
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024;
    i++;
  }
  return `${formatNumber(n, i ? 1 : 0)} ${units[i]}`;
}

export const todayIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
