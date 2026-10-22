// Modelo de filtros compartilhado entre dashboard e página Posts.
// Tudo vive na URL, para que links do dashboard abram a lista já filtrada e o F5 não perca nada.

import { SOURCE_VALUES } from "./sources";

const DAY_MS = 24 * 60 * 60 * 1000;

// Os períodos do backend são janelas móveis de N dias (StatsPeriod); "segment" é o rótulo do segmented
export const PERIOD_SHORTCUTS = [
  { value: "DAY", segment: "24 horas", label: "Últimas 24 horas", phrase: "nas últimas 24 horas", previous: "24 horas anteriores", days: 1 },
  { value: "WEEK", segment: "7 dias", label: "Últimos 7 dias", phrase: "nos últimos 7 dias", previous: "7 dias anteriores", days: 7 },
  { value: "MONTH", segment: "30 dias", label: "Últimos 30 dias", phrase: "nos últimos 30 dias", previous: "30 dias anteriores", days: 30 },
  { value: "YEAR", segment: "12 meses", label: "Últimos 12 meses", phrase: "nos últimos 12 meses", previous: "12 meses anteriores", days: 365 },
  { value: "ALL", segment: "Tudo", label: "Todo o período", phrase: "", previous: null, days: null },
];

export const RELEVANCE_VALUES = ["HIGH", "MEDIUM", "LOW"];

// Colunas ordenáveis na página Posts: só as que servem à análise.
// A primeira é o padrão (score, para não abrir em posts irrelevantes recentes).
export const SORT_FIELDS = ["SCORE", "DATE"];

// Primeiro clique começa do maior; o segundo inverte
export const DEFAULT_ORDER = { SCORE: "DESC", DATE: "DESC" };

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

const shortcutOf = (value) => PERIOD_SHORTCUTS.find((s) => s.value === value);

// { period: "WEEK" } ou { from: "2026-10-01", to: "2026-10-15" }
export function readPeriod(searchParams) {
  const from = searchParams.get("from");
  const to = searchParams.get("to");
  if (from && to && DATE_RE.test(from) && DATE_RE.test(to)) {
    return from <= to ? { from, to } : { from: to, to: from };
  }
  const period = searchParams.get("period");
  return { period: shortcutOf(period) ? period : "ALL" };
}

// ?source=TELEGRAM ou ?source=TELEGRAM,REDDIT; valores desconhecidos são ignorados
export function readSources(searchParams) {
  const raw = searchParams.get("source");
  if (!raw) return [];
  return [...new Set(raw.split(",").map((s) => s.trim().toUpperCase()))].filter((s) => SOURCE_VALUES.includes(s));
}

export function writeSources(params, sources) {
  if (sources.length) params.set("source", sources.join(","));
  else params.delete("source");
}

export function writePeriod(params, value) {
  params.delete("period");
  params.delete("from");
  params.delete("to");
  if (value.from) {
    params.set("from", value.from);
    params.set("to", value.to);
  } else if (value.period && value.period !== "ALL") {
    params.set("period", value.period);
  }
}

export function isCustomPeriod(value) {
  return Boolean(value.from);
}

export function isAllTime(value) {
  return !value.from && (!value.period || value.period === "ALL");
}

// Parâmetros no formato que os hooks/API esperam
export function periodToQuery(value) {
  if (value.from) return { from: `${value.from}T00:00:00`, to: `${value.to}T23:59:59` };
  return { period: value.period };
}

export function formatDateBR(isoDate) {
  const [year, month, day] = isoDate.split("-");
  return `${day}/${month}/${year}`;
}

// Texto do botão de período
export function periodLabel(value) {
  if (value.from) {
    return value.from === value.to
      ? formatDateBR(value.from)
      : `${formatDateBR(value.from)} – ${formatDateBR(value.to)}`;
  }
  return shortcutOf(value.period)?.label ?? "Todo o período";
}

// "15/10"; o ano (2 dígitos) só aparece se for diferente do atual: "15/10/25"
function formatShortDate(date) {
  const sameYear = date.getFullYear() === new Date().getFullYear();
  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", ...(sameYear ? {} : { year: "2-digit" }) });
}

// Versão curta do intervalo personalizado ("01/10 – 15/10"), para caber num segmento
export function shortRangeLabel(value) {
  const from = formatShortDate(new Date(`${value.from}T00:00:00`));
  if (value.from === value.to) return from;
  return `${from} – ${formatShortDate(new Date(`${value.to}T00:00:00`))}`;
}

// Complemento de frases como "Posts analisados nos últimos 7 dias"
export function periodPhrase(value) {
  if (value.from) {
    return value.from === value.to
      ? `em ${formatDateBR(value.from)}`
      : `de ${formatDateBR(value.from)} até ${formatDateBR(value.to)}`;
  }
  return shortcutOf(value.period)?.phrase ?? "";
}

// LocalDateTime sem fuso, no formato que o backend espera
export function toLocalIso(date) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function toDateKey(date) {
  return toLocalIso(date).slice(0, 10);
}

// Janela imediatamente anterior, com a mesma duração da selecionada
export function previousWindow(value) {
  if (value.from) {
    const start = new Date(`${value.from}T00:00:00`);
    const end = new Date(`${value.to}T23:59:59`);
    const length = end - start + 1000;
    return {
      from: toLocalIso(new Date(start - length)),
      to: toLocalIso(new Date(start - 1000)),
      label: "período anterior",
    };
  }

  const shortcut = shortcutOf(value.period);
  if (!shortcut?.days) return null;
  const now = Date.now();
  return {
    from: toLocalIso(new Date(now - 2 * shortcut.days * DAY_MS)),
    to: toLocalIso(new Date(now - shortcut.days * DAY_MS)),
    label: shortcut.previous,
  };
}

// Link para a página Posts com os filtros informados (mais o período atual).
// "source" (ex.: barra clicada) tem prioridade sobre "sources" (filtro do dashboard).
export function postsLink(period, { relevance, source, sources = [], search, sort, order } = {}) {
  const params = new URLSearchParams();
  writePeriod(params, period);
  if (relevance) params.set("relevance", relevance);
  if (source) params.set("source", source.toUpperCase());
  else writeSources(params, sources);
  if (search) params.set("search", search);
  if (sort) params.set("sort", sort);
  if (order) params.set("order", order);
  const query = params.toString();
  return query ? `/posts?${query}` : "/posts";
}
