import { StatTile } from '../../../components/common';
import { formatNumber } from '../../../utils/format';

/** Row counts shared by ImportSummaryResponse and ImportJobResponse. */
export function ImportCounts({ job }) {
  return (
    <div className="tiles">
      <StatTile label="Total rows" value={formatNumber(job.totalRows)} />
      <StatTile label="Auto-imported" value={formatNumber(job.autoImportedCount)} />
      <StatTile label="Needs validation" value={formatNumber(job.needsValidationCount)} />
      <StatTile label="Rejected" value={formatNumber(job.rejectedCount)} />
      <StatTile label="Invalid" value={formatNumber(job.invalidCount)} />
    </div>
  );
}
