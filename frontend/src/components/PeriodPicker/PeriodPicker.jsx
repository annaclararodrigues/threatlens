import { useCallback, useLayoutEffect, useRef, useState } from "react";
import { DayPicker } from "react-day-picker";
import { ptBR } from "react-day-picker/locale";
import "react-day-picker/style.css";
import { PiCalendarBlank, PiCaretDown, PiX } from "react-icons/pi";
import style from "./PeriodPicker.module.css";
import { useDismiss } from "../../hooks";
import { PERIOD_SHORTCUTS, isAllTime, isCustomPeriod, periodLabel, shortRangeLabel, toDateKey } from "../../utils/filters";

function toRange(value) {
  if (!value.from) return undefined;
  return { from: new Date(`${value.from}T00:00:00`), to: new Date(`${value.to}T00:00:00`) };
}

// compact: vira o último segmento do segmented do dashboard (que já tem os atalhos), só com o calendário.
// className/activeClassName vêm do segmented, para o segmento ter a mesma aparência dos outros.
export default function PeriodPicker({ value, onChange, align = "right", compact = false, className = "", activeClassName = "" }) {
  const showShortcuts = !compact;
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState(() => toRange(value));
  const wrapperRef = useRef(null);
  const popoverRef = useRef(null);
  // "align" é a preferência; se o popover não couber na tela, abre para o outro lado
  const [placement, setPlacement] = useState(align);

  useLayoutEffect(() => {
    if (!open || !popoverRef.current || !wrapperRef.current) return;
    const margin = 16;
    const width = popoverRef.current.offsetWidth;
    const trigger = wrapperRef.current.getBoundingClientRect();
    const fitsLeftAligned = trigger.left + width <= window.innerWidth - margin;
    const fitsRightAligned = trigger.right - width >= margin;

    if (align === "left") setPlacement(fitsLeftAligned || !fitsRightAligned ? "left" : "right");
    else setPlacement(fitsRightAligned || !fitsLeftAligned ? "right" : "left");
  }, [open, align]);

  const close = useCallback(() => setOpen(false), []);
  useDismiss(wrapperRef, open, close);

  const toggle = () => {
    if (!open) setDraft(toRange(value));
    setOpen((o) => !o);
  };

  const selectShortcut = (period) => {
    onChange({ period });
    setOpen(false);
  };

  const applyRange = () => {
    if (!draft?.from) return;
    onChange({ from: toDateKey(draft.from), to: toDateKey(draft.to ?? draft.from) });
    setOpen(false);
  };

  const draftLabel = draft?.from
    ? periodLabel({ from: toDateKey(draft.from), to: toDateKey(draft.to ?? draft.from) })
    : "Selecione o início e o fim";

  const active = compact ? isCustomPeriod(value) : !isAllTime(value);

  const clearButton = active && (
    <button
      type="button"
      className={compact ? style.compactClear : style.clear}
      onClick={() => selectShortcut("ALL")}
      aria-label="Limpar período"
      title="Limpar período"
    >
      <PiX aria-hidden="true" />
    </button>
  );

  const trigger = compact ? (
    <div className={`${className} ${active ? activeClassName : ""}`}>
      <button
        type="button"
        className={`${style.compactButton} ${active ? style.compactButtonWithClear : ""}`}
        onClick={toggle}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-label={active ? `Período personalizado: ${periodLabel(value)}` : undefined}
      >
        <PiCalendarBlank aria-hidden="true" />
        <span>{active ? shortRangeLabel(value) : "Personalizado"}</span>
      </button>
      {clearButton}
    </div>
  ) : (
    <div className={`${style.trigger} ${active ? style.triggerActive : ""}`}>
      <button
        type="button"
        className={style.triggerButton}
        onClick={toggle}
        aria-haspopup="dialog"
        aria-expanded={open}
      >
        <PiCalendarBlank aria-hidden="true" />
        <span>{periodLabel(value)}</span>
        <PiCaretDown className={style.caret} aria-hidden="true" />
      </button>
      {clearButton}
    </div>
  );

  return (
    <div className={style.wrapper} ref={wrapperRef}>
      {trigger}

      {open && (
        <div
          ref={popoverRef}
          className={`${style.popover} ${placement === "left" ? style.alignLeft : style.alignRight}`}
          role="dialog"
          aria-label="Selecionar período"
        >
          {showShortcuts && (
            <ul className={style.shortcuts}>
              {PERIOD_SHORTCUTS.map((shortcut) => {
                const selected = !isCustomPeriod(value) && value.period === shortcut.value;
                return (
                  <li key={shortcut.value}>
                    <button
                      type="button"
                      className={`${style.shortcut} ${selected ? style.shortcutActive : ""}`}
                      onClick={() => selectShortcut(shortcut.value)}
                    >
                      {shortcut.label}
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          <div className={style.calendar}>
            <DayPicker
              mode="range"
              locale={ptBR}
              selected={draft}
              onSelect={setDraft}
              defaultMonth={draft?.to ?? draft?.from ?? new Date()}
              disabled={{ after: new Date() }}
              endMonth={new Date()}
            />
            <div className={style.footer}>
              <span className={style.draftLabel}>{draftLabel}</span>
              <button type="button" className={style.apply} onClick={applyRange} disabled={!draft?.from}>
                Aplicar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
