import { formatSource } from "./sources";

export { formatSource };

// Remove emojis e normaliza espaços (apenas para exibição)
export function cleanText(text) {
  if (!text) return "";
  return text
    .replace(/[\p{Extended_Pictographic}️‍]/gu, "")
    .replace(/\s+/g, " ")
    .trim();
}

const relativeFormatter = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

// Acima disso a data relativa ("ano passado") deixa de ajudar; mostramos a data absoluta
const ABSOLUTE_AFTER_SECONDS = 7 * 24 * 60 * 60;

const RELATIVE_UNITS = [
  ["day", 24 * 60 * 60],
  ["hour", 60 * 60],
  ["minute", 60],
];

// "24/04 18:47"; o ano (2 dígitos) só aparece se for diferente do atual: "24/04/25 18:47"
export function formatCompactDate(isoDate) {
  if (!isoDate) return null;
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return null;
  const sameYear = date.getFullYear() === new Date().getFullYear();
  const day = date.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    ...(sameYear ? {} : { year: "2-digit" }),
  });
  const time = date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
  return `${day} ${time}`;
}

// "@canal" a partir do meta da fonte (o username pode vir com ou sem "@")
export function channelLabel(meta) {
  const username = meta?.channel_username?.replace(/^@+/, "");
  if (username) return `@${username}`;
  return meta?.channel_name ?? null;
}

// Origem do post: "Telegram – @canal"; fontes sem canal mostram só o nome da fonte
export function postOrigin(post) {
  const source = post?.source ? formatSource(post.source) : null;
  const channel = channelLabel(post?.meta);
  if (source && channel) return `${source} – ${channel}`;
  return source ?? channel;
}

export function formatDateTime(isoDate) {
  if (!isoDate) return null;
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function formatRelativeTime(isoDate) {
  if (!isoDate) return null;
  const date = new Date(isoDate);
  if (Number.isNaN(date.getTime())) return null;

  // Datas no futuro (relógios dessincronizados) contam como "agora"
  const seconds = Math.min(0, Math.round((date.getTime() - Date.now()) / 1000));
  if (-seconds >= ABSOLUTE_AFTER_SECONDS) return date.toLocaleDateString("pt-BR");

  for (const [unit, unitSeconds] of RELATIVE_UNITS) {
    if (Math.abs(seconds) >= unitSeconds) {
      return relativeFormatter.format(Math.round(seconds / unitSeconds), unit);
    }
  }
  return "agora";
}
