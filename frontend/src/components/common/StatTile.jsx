export function StatTile({ label, value, unit, foot }) {
  return (
    <div className="card tile">
      <div className="tile-label">{label}</div>
      <div className="tile-value">
        {value}
        {unit && <span className="tile-unit">{unit}</span>}
      </div>
      {foot && <div className="tile-foot">{foot}</div>}
    </div>
  );
}
