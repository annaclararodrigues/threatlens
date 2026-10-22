// Mesmos limiares usados pelo backend (TelegramRepository)
const LOW_THRESHOLD = 0.3;
const MEDIUM_THRESHOLD = 0.7;

export const RELEVANCE_LEVELS = {
  LOW: { label: "Baixa", colorVar: "--color-relevance-low", range: "score abaixo de 0.30" },
  MEDIUM: { label: "Média", colorVar: "--color-relevance-medium", range: "score de 0.30 a 0.69" },
  HIGH: { label: "Alta", colorVar: "--color-relevance-high", range: "score a partir de 0.70" },
};

export function getRelevanceLevel(score) {
  if (score == null) return null;
  if (score < LOW_THRESHOLD) return "LOW";
  if (score < MEDIUM_THRESHOLD) return "MEDIUM";
  return "HIGH";
}

