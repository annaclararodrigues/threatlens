import style from "./ScorePill.module.css";
import { RELEVANCE_LEVELS, getRelevanceLevel } from "../../utils/relevance";

const TONE_CLASS = {
  LOW: style.toneLow,
  MEDIUM: style.toneMedium,
  HIGH: style.toneHigh,
};

// Score colorido pela faixa de relevância (Alta = vermelho, Média = âmbar, Baixa = cinza)
export default function ScorePill({ score, showLabel = false }) {
  const level = getRelevanceLevel(score);
  if (!level) return <span className={`${style.pill} ${style.empty}`} title="Ainda não classificado">—</span>;

  const { label, range } = RELEVANCE_LEVELS[level];
  return (
    <span className={`${style.pill} ${TONE_CLASS[level]}`} title={`${label} relevância (${range})`}>
      {score.toFixed(2)}
      {showLabel && ` · ${label}`}
    </span>
  );
}
