import style from "./DoughnutChart.module.css";
import { Doughnut } from "react-chartjs-2";
import { Chart as ChartJS, ArcElement, Tooltip, Title } from "chart.js";
import { Link, useNavigate } from "react-router-dom";
import { useTheme } from "../../hooks";
import { RELEVANCE_LEVELS } from "../../utils/relevance";
import { cssVar } from "../../utils/theme";

ChartJS.register(ArcElement, Tooltip, Title);

export default function DoughnutChart({ low = 0, medium = 0, high = 0, linkForLevel }) {
  const isLight = useTheme();
  const navigate = useNavigate();
  const surfaceColor = cssVar(isLight ? "--color-surface-light" : "--color-surface");

  // Alta primeiro: é o que interessa ao analista
  const slices = [
    { level: "HIGH", value: high, color: cssVar(RELEVANCE_LEVELS.HIGH.colorVar) },
    { level: "MEDIUM", value: medium, color: cssVar(RELEVANCE_LEVELS.MEDIUM.colorVar) },
    // Baixa usa um roxo mais apagado que vermelho e âmbar, mas ainda legível
    { level: "LOW", value: low, color: cssVar("--color-chart-relevance-low") },
  ].map((slice) => ({ ...slice, ...RELEVANCE_LEVELS[slice.level] }));
  const total = low + medium + high;
  const pct = (value) => (total ? (value / total) * 100 : 0);

  const data = {
    labels: slices.map((s) => `${s.label} relevância`),
    datasets: [
      {
        label: "Quantidade de posts",
        data: slices.map((s) => s.value),
        backgroundColor: slices.map((s) => s.color),
        borderColor: surfaceColor,
        borderWidth: 3,
        borderRadius: 6,
        spacing: 2,
        hoverOffset: 4,
      },
    ],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: "72%",
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => ` ${ctx.parsed.toLocaleString("pt-BR")} posts (${slices[ctx.dataIndex].range})`,
        },
      },
    },
    onClick: (_event, elements) => {
      if (linkForLevel && elements.length) navigate(linkForLevel(slices[elements[0].index].level));
    },
    onHover: (event, elements) => {
      event.native.target.style.cursor = linkForLevel && elements.length ? "pointer" : "default";
    },
  };

  return (
    <div className={style.doughnutChartContainer}>
      <div className={style.header}>
        <h2 className={style.description}>Distribuição por relevância</h2>
        <p className={style.subtitle}>{total.toLocaleString("pt-BR")} posts no período</p>
      </div>

      <div className={style.chartWrapper}>
        <Doughnut data={data} options={options} />
      </div>

      <ul className={style.legend}>
        {slices.map((slice) => (
          <li key={slice.level}>
            <Link
              to={linkForLevel ? linkForLevel(slice.level) : "/posts"}
              className={style.legendItem}
              title={`${slice.label} relevância: ${slice.range}. Clique para ver os posts.`}
            >
              <span className={style.legendDot} style={{ backgroundColor: slice.color }} />
              <span className={style.legendLabel}>{slice.label}</span>
              <span className={style.legendValue}>{slice.value.toLocaleString("pt-BR")}</span>
              <span className={style.legendBar}>
                <span style={{ width: `${pct(slice.value)}%`, backgroundColor: slice.color }} />
              </span>
              <span className={style.legendPct}>{pct(slice.value).toFixed(0)}%</span>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
