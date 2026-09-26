/** Edits a string[] (partNames) as a comma-separated text input. */
export function PartNamesInput({ value, onChange, ...props }) {
  return (
    <input
      className="input"
      placeholder="e.g. Bearing 6204, V-belt A42"
      value={(value ?? []).join(', ')}
      onChange={(e) =>
        onChange(
          e.target.value
            .split(',')
            .map((s) => s.trimStart())
            .filter((s, i, arr) => s || i === arr.length - 1),
        )
      }
      onBlur={() => onChange((value ?? []).map((s) => s.trim()).filter(Boolean))}
      {...props}
    />
  );
}
