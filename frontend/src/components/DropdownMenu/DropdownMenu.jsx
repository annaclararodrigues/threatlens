import { useCallback, useRef, useState } from "react";
import style from "./DropdownMenu.module.css";
import { useDismiss } from "../../hooks";

// Menu de ações. Fecha ao clicar fora, no Esc ou ao escolher um item.
// items: [{ label, icon, onClick, danger, separated }]; separated desenha uma linha acima do item.
export default function DropdownMenu({ trigger, triggerClassName = "", label, header, items, align = "right" }) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef(null);

  const close = useCallback(() => setOpen(false), []);
  useDismiss(wrapperRef, open, close);

  const select = (item) => {
    setOpen(false);
    item.onClick();
  };

  return (
    <div className={style.wrapper} ref={wrapperRef}>
      <button
        type="button"
        className={triggerClassName}
        onClick={() => setOpen((o) => !o)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={label}
        title={label}
      >
        {trigger}
      </button>

      {open && (
        <div className={`${style.menu} ${align === "left" ? style.alignLeft : style.alignRight}`} role="menu">
          {header && <div className={style.header}>{header}</div>}
          {items.map((item) => (
            <button
              key={item.label}
              type="button"
              role="menuitem"
              className={`${style.item} ${item.danger ? style.danger : ""} ${item.separated ? style.separated : ""}`}
              onClick={() => select(item)}
            >
              {item.icon && <span className={style.icon} aria-hidden="true">{item.icon}</span>}
              {item.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
