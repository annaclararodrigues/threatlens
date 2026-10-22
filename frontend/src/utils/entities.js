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
};

function humanizeKey(key) {
  const text = key.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/[_-]+/g, " ").trim();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function sourceMetaEntries(meta) {
  return Object.entries(meta ?? {})
    .filter(([, value]) => value != null && value !== "" && typeof value !== "object")
    .map(([key, value]) => ({ key, label: SOURCE_META_LABELS[key] ?? humanizeKey(key), value: String(value) }));
}
