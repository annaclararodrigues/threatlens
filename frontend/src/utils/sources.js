// Fontes de coleta. Espelha o enum PostSource do backend: ao ativar uma fonte nova,
// basta adicioná-la aqui (e, se quiser ícone, em SourceBadge).
export const SOURCES = [
  { value: "TELEGRAM", label: "Telegram" },
];

const LABELS = Object.fromEntries(SOURCES.map((s) => [s.value, s.label]));

export const SOURCE_VALUES = SOURCES.map((s) => s.value);

// Aceita o enum ("TELEGRAM") ou o nome que vem nas estatísticas ("telegram")
export function formatSource(source) {
  if (!source) return "-";
  return LABELS[source.toUpperCase()] ?? source;
}
