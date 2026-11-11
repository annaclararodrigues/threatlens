import { useState } from "react";
import style from "./BarChart.module.css"
import { Bar } from "react-chartjs-2";
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend } from "chart.js";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../../hooks";
import { cssVar } from "../../utils/theme";
import { formatSource } from "../../utils/format";
ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

const MAX_SOURCES = 8;
const MAX_LABEL_LENGTH = 22;

// Critérios do ranking: total de posts ou posts relevantes (empate decidido pelo total)
const SORTS = [
  { value: "total", label: "Total", compare: (a, b) => b.postCount - a.postCount },
  { value: "relevant", label: "Relevantes", compare: (a, b) => b.relevant - a.relevant || b.postCount - a.postCount },
];

function truncate(text) {
  return text.length > MAX_LABEL_LENGTH ? `${text.slice(0, MAX_LABEL_LENGTH - 1)}…` : text;
}

const formatNumber = (n) => n.toLocaleString("pt-BR");

// Barra empilhada por fonte: o trecho vermelho são os posts relevantes, o restante completa o total
export default function BarChart({ sources = [], label = "", linkForSource }) {
  const navigate = useNavigate();
  const isLight = useTheme();
  const [sortBy, setSortBy] = useState("total");
  const textColor = cssVar(isLight ? "--color-text-light" : "--color-text");
  const mutedColor = cssVar(isLight ? "--color-text-muted-light" : "--color-text-muted");
  const surfaceColor = cssVar(isLight ? "--color-surface-light" : "--color-surface");
  const gridColor = cssVar("--color-border");

  const compare = SORTS.find((s) => s.value === sortBy).compare;
  const ranking = sources
    .filter((s) => s.postCount > 0)
    .map((s) => ({ ...s, relevant: s.relevantCount ?? 0 }))
    .sort(compare)
    .slice(0, MAX_SOURCES);

  const data = {
    labels: ranking.map((s) => formatSource(s.sourceName)),
    datasets: [
      {
        label: "Relevantes",
        data: ranking.map((s) => s.relevant),
        backgroundColor: cssVar("--color-relevance-high"),
        // Borda na cor do fundo = 2px de respiro entre os dois trechos da barra
        borderColor: surfaceColor,
        borderWidth: { right: 2 },
        barThickness: 18,
      },
      {
        label: "Demais posts",
        data: ranking.map((s) => s.postCount - s.relevant),
        backgroundColor: cssVar("--color-accent"),
        hoverBackgroundColor: cssVar("--color-accent-hover"),
        borderRadius: 4,
        barThickness: 18,
      },
    ],
  };

  const options = {
    indexAxis: "y",
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "index", axis: "y", intersect: true },
    // Clique na barra abre a página Posts filtrada por aquela fonte
    onClick: (_event, elements) => {
      if (linkForSource && elements.length) navigate(linkForSource(ranking[elements[0].index].sourceName));
    },
    onHover: (event, elements) => {
      event.native.target.style.cursor = linkForSource && elements.length ? "pointer" : "default";
    },
    plugins: {
      legend: {
        position: "top",
        align: "start",
        labels: { color: mutedColor, boxWidth: 10, boxHeight: 10, useBorderRadius: true, borderRadius: 2 },
      },
      tooltip: {
        callbacks: {
          label: (item) => ` ${item.dataset.label}: ${formatNumber(item.raw)}`,
          footer: (items) => {
            const source = ranking[items[0]?.dataIndex];
            if (!source) return "";
            const pct = Math.round((source.relevant / source.postCount) * 100);
            const lines = [`Total: ${formatNumber(source.postCount)} · ${pct}% relevantes`];
            if (linkForSource) lines.push("Clique para ver os posts");
            return lines;
          },
        },
      },
    },
    scales: {
      x: {
        stacked: true,
        beginAtZero: true,
        border: { display: false },
        grid: { color: gridColor },
        ticks: { color: mutedColor, precision: 0 },
      },
      y: {
        stacked: true,
        border: { display: false },
        grid: { display: false },
        ticks: {
          color: textColor,
          callback(value) {
            return truncate(this.getLabelForValue(value));
          },
        },
      },
    },
  };

  return (
    <div className={style.barChartContainer}>
      <div className={style.header}>
        <h2 className={style.description}>
          Principais fontes
          {label && <span className={style.period}> ({label})</span>}
        </h2>
        <div className={style.sortToggle} role="radiogroup" aria-label="Ordenar fontes por">
          {SORTS.map(({ value, label: sortLabel }) => (
            <button
              key={value}
              type="button"
              role="radio"
              aria-checked={sortBy === value}
              className={`${style.sortOption} ${sortBy === value ? style.sortOptionActive : ""}`}
              onClick={() => setSortBy(value)}
            >
              {sortLabel}
            </button>
          ))}
        </div>
      </div>
      {ranking.length ? (
        <div className={style.chartWrapper}>
          <Bar data={data} options={options} />
        </div>
      ) : (
        <p className={style.empty}>Nenhum post no período.</p>
      )}
    </div>
  );
};
