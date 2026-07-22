import { Input, Select } from "./ui";

export interface RangeValue {
  preset: string;
  from?: string;
  to?: string;
}

const PRESETS = [
  { value: "LAST_7D", label: "Last 7 days" },
  { value: "LAST_30D", label: "Last 30 days" },
  { value: "LAST_90D", label: "Last 90 days" },
  { value: "THIS_MONTH", label: "This month" },
  { value: "THIS_YEAR", label: "This year" },
  { value: "CUSTOM", label: "Custom range" },
];

export function DateRangePicker({
  value,
  onChange,
}: {
  value: RangeValue;
  onChange: (v: RangeValue) => void;
}) {
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="w-44">
        <Select
          label="Period"
          value={value.preset}
          onChange={(e) => onChange({ ...value, preset: e.target.value })}
        >
          {PRESETS.map((p) => (
            <option key={p.value} value={p.value}>
              {p.label}
            </option>
          ))}
        </Select>
      </div>
      {value.preset === "CUSTOM" && (
        <>
          <div className="w-40">
            <Input
              type="date"
              label="From"
              value={value.from ?? ""}
              onChange={(e) => onChange({ ...value, from: e.target.value })}
            />
          </div>
          <div className="w-40">
            <Input
              type="date"
              label="To"
              value={value.to ?? ""}
              onChange={(e) => onChange({ ...value, to: e.target.value })}
            />
          </div>
        </>
      )}
    </div>
  );
}
