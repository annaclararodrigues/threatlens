// Papéis do backend (enum Role) com o nome exibido na interface
const ROLE_LABELS = {
  ADMIN: "Admin",
  USER: "Analista",
};

export function roleLabel(role) {
  return ROLE_LABELS[role] ?? role ?? "-";
}

// "Anna Clara" -> "AC"; "anna" -> "A"
export function initials(name) {
  const words = (name ?? "").trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return "?";
  const first = words[0][0];
  const last = words.length > 1 ? words[words.length - 1][0] : "";
  return `${first}${last}`.toUpperCase();
}
