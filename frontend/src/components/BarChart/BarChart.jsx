import style from "./BarChart.module.css"
import { Bar } from "react-chartjs-2";
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, } from "chart.js";
import { useNavigate } from "react-router-dom";
import { useTheme } from "../../hooks";
import { cssVar } from "../../utils/theme";
import { formatSource } from "../../utils/format";
ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip);

const MAX_SOURCES = 8;
const MAX_LABEL_LENGTH = 22;

function truncate(text) {
  return text.length > MAX_LABEL_LENGTH ? `${text.slice(0, MAX_LABEL_LENGTH - 1)}…` : text;
}

export default function BarChart({ sources = [], label = "", linkForSource }) {
  const navigate = useNavigate();
  const isLight = useTheme();
  const textColor = cssVar(isLight ? "--color-text-light" : "--color-text");
  const mutedColor = cssVar(isLight ? "--color-text-muted-light" : "--color-text-muted");
  const gridColor = cssVar("--color-border");

  const ranking = sources
    .filter((s) => s.postCount > 0)
    .sort((a, b) => b.postCount - a.postCount)
    .slice(0, MAX_SOURCES);

  const data = {
    labels: ranking.map((s) => formatSource(s.sourceName)),
    datasets: [
      {
        label: "Posts coletados",
        data: ranking.map((s) => s.postCount),
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
    // Clique na barra abre a página Posts filtrada por aquela fonte
    onClick: (_event, elements) => {
      if (linkForSource && elements.length) navigate(linkForSource(ranking[elements[0].index].sourceName));
    },
    onHover: (event, elements) => {
      event.native.target.style.cursor = linkForSource && elements.length ? "pointer" : "default";
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          footer: () => (linkForSource ? "Clique para ver os posts" : ""),
        },
      },
    },
    scales: {
      x: {
        beginAtZero: true,
        border: { display: false },
        grid: { color: gridColor },
        ticks: { color: mutedColor, precision: 0 },
      },
      y: {
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
      <h2 className={style.description}>
        Principais fontes
        {label && <span className={style.period}> ({label})</span>}
      </h2>
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
