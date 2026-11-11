// Mesmos limiares usados pelo backend (PostsRepository)
const LOW_THRESHOLD = 0.3;
const MEDIUM_THRESHOLD = 0.7;

export const RELEVANCE_LEVELS = {
  LOW: { label: "Baixa", colorVar: "--color-relevance-low", range: "score abaixo de 0.30" },
  MEDIUM: { label: "Média", colorVar: "--color-relevance-medium", range: "score de 0.30 a 0.69" },
  HIGH: { label: "Alta", colorVar: "--color-relevance-high", range: "score a partir de 0.70" },
};

// Trunca (não arredonda) em 2 casas: 0.6973 vira "0.69", não "0.70", para o número exibido
// cair sempre na mesma faixa da relevância. O epsilon evita que 0.29 * 100 = 28.999... vire "0.28".
export function formatScore(score) {
  if (score == null) return "";
  return (Math.floor(score * 100 + 1e-9) / 100).toFixed(2);
}

export function getRelevanceLevel(score) {
  if (score == null) return null;
  if (score < LOW_THRESHOLD) return "LOW";
  if (score < MEDIUM_THRESHOLD) return "MEDIUM";
  return "HIGH";
}

