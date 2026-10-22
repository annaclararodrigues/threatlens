// Lê o valor atual de um token CSS (respeita os overrides de body.light)
export function cssVar(name) {
  return getComputedStyle(document.body).getPropertyValue(name).trim();
}

const THEME_KEY = "theme";

// Tema salvo pelo usuário; escuro é o padrão
export function isStoredThemeLight() {
  try {
    return localStorage.getItem(THEME_KEY) === "light";
  } catch {
    return false;
  }
}

export function applyTheme(isLight) {
  document.body.classList.toggle("light", isLight);
  try {
    localStorage.setItem(THEME_KEY, isLight ? "light" : "dark");
  } catch {
    // Storage indisponível (ex.: modo privado): o tema vale só nesta sessão
  }
}
