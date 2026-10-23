import { PiArrowDown, PiCaretRight } from "react-icons/pi";
import style from "./AllPostsTable.module.css";
import ScorePill from "../ScorePill/ScorePill";
import SourceBadge from "../SourceBadge/SourceBadge";
import { cleanText, formatCompactDate, formatDateTime, formatSource, shortPostId } from "../../utils/format";

// sortField: só colunas que servem à análise são ordenáveis
const COLUMNS = [
  { key: "id", label: "ID", className: style.colId, hideWhenCompact: true },
  { key: "date", label: "Data", sortField: "DATE", className: style.colDate, hideWhenCompact: true },
  { key: "source", label: "Fonte", className: style.colSource },
  { key: "score", label: "Score", sortField: "SCORE", className: style.colScore },
  { key: "content", label: "Conteúdo" },
  { key: "open", label: "", className: style.colOpen },
];

function SortHeader({ label, field, sort, order, onSort }) {
  const active = sort === field;
  return (
    <button
      type="button"
      className={style.thSort}
      onClick={() => onSort(field)}
      title={active ? `Ordenado por ${label.toLowerCase()} (${order === "ASC" ? "crescente" : "decrescente"})` : `Ordenar por ${label.toLowerCase()}`}
    >
      {label}
      <PiArrowDown
        className={`${style.arrow} ${active ? style.arrowActive : ""} ${active && order === "ASC" ? style.arrowUp : ""}`}
        aria-hidden="true"
      />
    </button>
  );
}

export default function AllPostsTable({ posts = [], loading = false, sort, order, onSort, selectedId, onSelect, compact = false }) {
  // "Carregando..." só na primeira carga; depois, as linhas atuais ficam visíveis (esmaecidas)
  // até a resposta chegar, para a tabela não piscar a cada página ou filtro
  if (!posts.length) {
    return <p className={style.message}>{loading ? "Carregando..." : "Nenhum post encontrado."}</p>;
  }

  const columns = COLUMNS.filter((col) => !(compact && col.hideWhenCompact));

  return (
    <div className={`${style.tableWrapper} ${loading ? style.refreshing : ""}`} aria-busy={loading}>
      {loading && <span className={style.progress} role="status" aria-label="Carregando posts" />}
      <table className={style.postsTable}>
        <colgroup>
          {columns.map((col) => <col key={col.key} className={col.className} />)}
        </colgroup>
        <thead>
          <tr>
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                aria-sort={col.sortField && sort === col.sortField ? (order === "ASC" ? "ascending" : "descending") : undefined}
              >
                {col.sortField && onSort ? (
                  <SortHeader label={col.label} field={col.sortField} sort={sort} order={order} onSort={onSort} />
                ) : (
                  col.label
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {posts.map((post) => {
            const selected = String(post.id) === String(selectedId);
            const text = cleanText(post.title ?? post.content);
  
            return (
              <tr
                key={post.id}
                className={`${style.row} ${selected ? style.selectedRow : ""}`}
                onClick={() => onSelect?.(post)}
                aria-selected={selected}
              >
                {!compact && <td className={style.id} title={String(post.id)}>{shortPostId(post.id)}</td>}
                {!compact && (
                  <td className={style.date} title={formatDateTime(post.createdAt) ?? undefined}>
                    {formatCompactDate(post.createdAt) ?? "—"}
                  </td>
                )}
                <td className={style.source}>
                  <span className={style.sourceCell}>
                    <SourceBadge source={post.source} />
                    {formatSource(post.source)}
                  </span>
                </td>
                <td className={style.score}><ScorePill score={post.classification?.score} /></td>
                <td className={style.content} title={text}>{text || "Sem conteúdo"}</td>
                <td className={style.open}>
                  <button
                    type="button"
                    className={style.openButton}
                    onClick={(e) => { e.stopPropagation(); onSelect?.(post); }}
                    aria-label="Abrir detalhes do post"
                  >
                    <PiCaretRight aria-hidden="true" />
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
