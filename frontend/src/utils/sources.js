// Fontes de coleta. Espelha o enum PostSource do backend: ao ativar uma fonte nova,
// basta adicioná-la aqui. "kind" define o ícone em SourceBadge (fontes sem ícone próprio).
export const SOURCES = [
  { value: "TELEGRAM", label: "Telegram", kind: "telegram" },
  { value: "NULLEDBB", label: "NulledBB", kind: "forum" },
  { value: "NIFLHEIM", label: "Niflheim", kind: "forum" },
  { value: "HACKONOLOGY", label: "Hackonology", kind: "forum" },
  { value: "LEGITCARDERS", label: "LegitCarders", kind: "forum" },
  { value: "RADDLE", label: "Raddle", kind: "forum" },
  { value: "HA", label: "HA", kind: "qa" },
  { value: "DEEP", label: "Deep", kind: "qa" },
];

const BY_VALUE = Object.fromEntries(SOURCES.map((s) => [s.value, s]));

export const SOURCE_VALUES = SOURCES.map((s) => s.value);

// Aceita o enum ("TELEGRAM") ou o nome que vem nas estatísticas ("telegram")
export function formatSource(source) {
  if (!source) return "-";
  return BY_VALUE[source.toUpperCase()]?.label ?? source;
}

export function sourceKind(source) {
  return BY_VALUE[source?.toUpperCase()]?.kind ?? null;
}
