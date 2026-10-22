import style from "./DashboardGreeting.module.css";
import { useAuth } from "../../context/AuthContext";
import { useCollectionStatus } from "../../hooks";
import { formatDateTime, formatRelativeTime } from "../../utils/format";

// Sem mensagem nova há mais que isso, a "última coleta" vira um aviso (âmbar).
// Não afirma que o coletor caiu: canais podem só estar quietos (3 dias cobre um fim de semana).
const STALE_AFTER_DAYS = 3;
const STALE_AFTER_MS = STALE_AFTER_DAYS * 24 * 60 * 60 * 1000;

function firstName(username) {
  return username?.trim().split(/\s+/)[0] ?? "";
}

// Subtítulo do dashboard: saudação + quando o coletor gravou a última mensagem
export default function DashboardGreeting() {
  const { user } = useAuth();
  const { status, error } = useCollectionStatus();
  const name = firstName(user?.username);
  const greeting = name ? `Olá, ${name}!` : "Olá!";

  // Enquanto carrega (ou se a API falhar) mostra só a saudação, sem inventar status
  if (!status || error) return greeting;

  const last = status.lastCollectedAt;
  if (!last) {
    return (
      <>
        {greeting}
        <span className={style.separator} aria-hidden="true">·</span>
        <span className={style.stale}>Nenhuma coleta registrada</span>
      </>
    );
  }

  const stale = Date.now() - new Date(last).getTime() > STALE_AFTER_MS;
  return (
    <>
      {greeting}
      <span className={style.separator} aria-hidden="true">·</span>
      <span
        className={stale ? style.stale : undefined}
        title={stale ? `Nenhuma mensagem nova nos últimos ${STALE_AFTER_DAYS} dias (${formatDateTime(last)})` : formatDateTime(last)}
      >
        Última coleta: {formatRelativeTime(last)}
      </span>
    </>
  );
}
