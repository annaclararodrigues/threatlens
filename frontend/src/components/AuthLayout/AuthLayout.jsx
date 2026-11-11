import style from "./AuthLayout.module.css";
import logoDark from "../../assets/brand/logo-dark.png";
import logoLight from "../../assets/brand/logo-light.png";

// Moldura das telas de autenticação (login, cadastro, código, senha): card centralizado com a logo.
// As páginas importam AuthLayout.module.css para os campos (form, field, label, link...).
export default function AuthLayout({ subtitle, children, footer }) {
  return (
    <main className={style.page}>
      <div className={style.card}>
        {/* As duas versões ficam no HTML; o CSS mostra a do tema ativo, como na sidebar */}
        <div className={style.brand}>
          <img src={logoDark} alt="ThreatLens" className={`${style.logo} ${style.logoDark}`} />
          <img src={logoLight} alt="ThreatLens" className={`${style.logo} ${style.logoLight}`} />
        </div>

        {subtitle && <p className={style.subtitle}>{subtitle}</p>}

        {children}

        {footer && <div className={style.footer}>{footer}</div>}
      </div>
    </main>
  );
}

// Botão principal: spinner e desabilitado durante a requisição (evita clique duplo)
export function AuthSubmit({ loading, loadingLabel, children, disabled = false }) {
  return (
    <button type="submit" className={style.submit} disabled={loading || disabled} aria-busy={loading}>
      {loading && <span className={style.spinner} aria-hidden="true" />}
      {loading ? loadingLabel : children}
    </button>
  );
}

export function AuthError({ children }) {
  if (!children) return null;
  return (
    <p className={style.error} role="alert">
      {children}
    </p>
  );
}
