import { useCallback, useRef, useState } from "react";
import { PiCaretDown, PiCheck, PiX, PiStack } from "react-icons/pi";
import style from "./SourceFilter.module.css";
import SourceBadge from "../SourceBadge/SourceBadge";
import { SOURCES, formatSource } from "../../utils/sources";
import { useDismiss } from "../../hooks";

function triggerLabel(selected) {
  if (selected.length === 0) return "Todas as fontes";
  if (selected.length === 1) return formatSource(selected[0]);
  return `${selected.length} fontes`;
}

// Seleção múltipla de fontes. Lista vazia = todas as fontes.
// align="right": o popover abre para a esquerda (para quando o filtro fica na borda direita)
export default function SourceFilter({ value = [], onChange, align = "left" }) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef(null);

  const close = useCallback(() => setOpen(false), []);
  useDismiss(wrapperRef, open, close);

  const toggle = (source) => {
    const next = value.includes(source) ? value.filter((s) => s !== source) : [...value, source];
    onChange(next);
  };

  const active = value.length > 0;

  return (
    <div className={style.wrapper} ref={wrapperRef}>
      <div className={`${style.trigger} ${active ? style.triggerActive : ""}`}>
        <button
          type="button"
          className={style.triggerButton}
          onClick={() => setOpen((o) => !o)}
          aria-haspopup="listbox"
          aria-expanded={open}
        >
          {value.length === 1 ? <SourceBadge source={value[0]} size={18} /> : <PiStack aria-hidden="true" />}
          <span>{triggerLabel(value)}</span>
          <PiCaretDown className={style.caret} aria-hidden="true" />
        </button>
        {active && (
          <button
            type="button"
            className={style.clear}
            onClick={() => onChange([])}
            aria-label="Limpar filtro de fonte"
            title="Limpar filtro de fonte"
          >
            <PiX aria-hidden="true" />
          </button>
        )}
      </div>

      {open && (
        <ul className={`${style.popover} ${align === "right" ? style.alignRight : ""}`} role="listbox" aria-multiselectable="true" aria-label="Fontes">
          {SOURCES.map(({ value: source, label }) => {
            const selected = value.includes(source);
            return (
              <li key={source}>
                <button
                  type="button"
                  role="option"
                  aria-selected={selected}
                  className={`${style.option} ${selected ? style.optionSelected : ""}`}
                  onClick={() => toggle(source)}
                >
                  <span className={style.check} aria-hidden="true">{selected && <PiCheck />}</span>
                  <SourceBadge source={source} size={20} />
                  {label}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
