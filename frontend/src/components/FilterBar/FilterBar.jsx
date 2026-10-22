import style from "./FilterBar.module.css";
import PeriodPicker from "../PeriodPicker/PeriodPicker";
import SourceFilter from "../SourceFilter/SourceFilter";
import { PERIOD_SHORTCUTS, isCustomPeriod } from "../../utils/filters";

// Toolbar do dashboard: período (ocupa o espaço livre) e fonte, na largura toda
export default function FilterBar({ value, onChange, sources, onSourcesChange }) {
  const custom = isCustomPeriod(value);

  return (
    <div className={style.filterContainer}>
      <div className={style.segmented} role="group" aria-label="Período">
        {PERIOD_SHORTCUTS.map((shortcut) => {
          const selected = !custom && value.period === shortcut.value;
          return (
            <button
              key={shortcut.value}
              type="button"
              className={`${style.segment} ${selected ? style.segmentSelected : ""}`}
              aria-pressed={selected}
              title={shortcut.label}
              onClick={() => onChange({ period: shortcut.value })}
            >
              {shortcut.segment}
            </button>
          );
        })}
        <span className={style.divider} aria-hidden="true" />
        <PeriodPicker
          value={value}
          onChange={onChange}
          align="left"
          compact
          className={style.pickerSegment}
          activeClassName={style.segmentSelected}
        />
      </div>
      <SourceFilter value={sources} onChange={onSourcesChange} align="right" />
    </div>
  );
}
