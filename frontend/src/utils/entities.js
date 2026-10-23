// Tratamento do texto e dos metadados de fonte de um post

const EMOJI_RE = /[\p{Extended_Pictographic}\uFE0F\u200D]/gu;

// Remove emojis mas mantém as quebras de linha (para a mensagem original)
export function stripEmojis(text) {
  if (!text) return "";
  return text
    .replace(EMOJI_RE, "")
    .split("\n")
    .map((line) => line.replace(/[ \t]+/g, " ").trim())
    .join("\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

// Rótulos dos metadados por fonte; chaves desconhecidas aparecem humanizadas
const SOURCE_META_LABELS = {
  channel_name: "Canal",
  channel_username: "Usuário",
  telegram_channel_id: "ID do canal",
  message_id: "ID da mensagem",
  views: "Visualizações",
  forwarded_from: "Encaminhada de",
  author: "Autor",
  thread: "Thread",
  site: "Site",
  forum: "Fórum",
  link: "Link",
  language: "Idioma",
  tags: "Tags",
  votes: "Votos",
  up_votes: "Votos positivos",
  down_votes: "Votos negativos",
  points: "Pontos",
};

// Idiomas das tabelas de HA e Deep, como o backend envia em meta.language
const LANGUAGE_LABELS = {
  en: "Inglês",
  "en-US": "Inglês",
  "es-ES": "Espanhol",
  "pt-BR": "Português",
};

function humanizeKey(key) {
  const text = key.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/[_-]+/g, " ").trim();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

function formatMetaValue(key, value) {
  if (key === "language") return LANGUAGE_LABELS[value] ?? value;
  return String(value);
}

export function sourceMetaEntries(meta) {
  return Object.entries(meta ?? {})
    .filter(([, value]) => value != null && value !== "" && typeof value !== "object")
    .map(([key, value]) => ({ key, label: SOURCE_META_LABELS[key] ?? humanizeKey(key), value: formatMetaValue(key, value) }));
}

// Só links http(s) viram âncora: o conteúdo vem de fontes externas e não é confiável
export function safeExternalUrl(value) {
  if (typeof value !== "string") return null;
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}

// Categoria pode vir como lista ("a, b"), trilha ("a > b") ou array do Postgres ("{a,b}")
export function splitCategories(category) {
  if (!category) return [];
  const items = String(category)
    .replace(/^\{(.*)\}$/s, "$1")
    .split(/\s*(?:[,;|»>])\s*/)
    .map((item) => item.replace(/^"(.*)"$/, "$1").trim())
    .filter(Boolean);
  return [...new Set(items)];
}
