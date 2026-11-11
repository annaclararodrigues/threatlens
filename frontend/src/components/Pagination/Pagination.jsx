import { useState } from "react";
import { PiCaretDoubleLeft, PiCaretDoubleRight, PiCaretLeft, PiCaretRight } from "react-icons/pi";
import style from "./Pagination.module.css";

// Até isso, todas as páginas aparecem; acima, só a primeira, a última e as vizinhas da atual
const MAX_WITHOUT_GAPS = 7;

const formatNumber = (n) => n.toLocaleString("pt-BR");

// Páginas (base 0) a exibir, com "gap" onde há reticências: 0 … 5 6 [7] 8 9 … 1040
function pageItems(current, total) {
  if (total <= MAX_WITHOUT_GAPS) return Array.from({ length: total }, (_, i) => i);

  // Perto das pontas o bloco do meio encosta nelas, para a quantidade de botões não variar
  let start = Math.max(1, current - 1);
  let end = Math.min(total - 2, current + 1);
  if (current <= 3) {
    start = 1;
    end = 4;
  } else if (current >= total - 4) {
    start = total - 5;
    end = total - 2;
  }

  const items = [0];
  if (start > 1) items.push("gap-start");
  for (let i = start; i <= end; i++) items.push(i);
  if (end < total - 2) items.push("gap-end");
  items.push(total - 1);
  return items;
}

function NavButton({ label, disabled, onClick, children }) {
  return (
    <button type="button" className={style.pageBtn} disabled={disabled} onClick={onClick} aria-label={label} title={label}>
      {children}
    </button>
  );
}

// page é base 0 (como a API); os números exibidos começam em 1
export default function Pagination({ page, totalPages, onChange }) {
  const [jumpValue, setJumpValue] = useState("");

  if (totalPages <= 1) return null;

  const isFirst = page === 0;
  const isLast = page >= totalPages - 1;

  const jump = (e) => {
    e.preventDefault();
    const target = Number.parseInt(jumpValue, 10);
    if (Number.isFinite(target)) {
      onChange(Math.min(Math.max(target, 1), totalPages) - 1);
    }
    setJumpValue("");
  };

  return (
    <nav className={style.pagination} aria-label="Paginação">
      <NavButton label="Primeira página" disabled={isFirst} onClick={() => onChange(0)}>
        <PiCaretDoubleLeft aria-hidden="true" />
      </NavButton>
      <NavButton label="Página anterior" disabled={isFirst} onClick={() => onChange(page - 1)}>
        <PiCaretLeft aria-hidden="true" />
      </NavButton>

      <ol className={style.pages}>
        {pageItems(page, totalPages).map((item) =>
          typeof item === "string" ? (
            <li key={item} className={style.gap} aria-hidden="true">…</li>
          ) : (
            <li key={item}>
              <button
                type="button"
                className={`${style.pageBtn} ${style.pageNumber} ${item === page ? style.pageCurrent : ""}`}
                onClick={() => onChange(item)}
                aria-current={item === page ? "page" : undefined}
                aria-label={`Página ${formatNumber(item + 1)}`}
              >
                {formatNumber(item + 1)}
              </button>
            </li>
          )
        )}
      </ol>

      <NavButton label="Próxima página" disabled={isLast} onClick={() => onChange(page + 1)}>
        <PiCaretRight aria-hidden="true" />
      </NavButton>
      <NavButton label="Última página" disabled={isLast} onClick={() => onChange(totalPages - 1)}>
        <PiCaretDoubleRight aria-hidden="true" />
      </NavButton>

      <form className={style.jump} onSubmit={jump}>
        <label>
          Ir para
          <input
            type="number"
            min={1}
            max={totalPages}
            inputMode="numeric"
            value={jumpValue}
            onChange={(e) => setJumpValue(e.target.value)}
            placeholder={formatNumber(page + 1)}
            aria-label={`Ir para a página (1 a ${formatNumber(totalPages)})`}
          />
        </label>
      </form>
    </nav>
  );
}
