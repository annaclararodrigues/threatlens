import { useEffect, useMemo, useState } from "react";
import { PiArrowUp, PiArrowDown, PiX, PiCopy, PiCheck, PiTelegramLogo, PiArrowSquareOut } from "react-icons/pi";
import style from "./PostDrawer.module.css";
import ScorePill from "../ScorePill/ScorePill";
import { RELEVANCE_LEVELS, getRelevanceLevel } from "../../utils/relevance";
import { formatDateTime, formatSource } from "../../utils/format";
import { sourceMetaEntries, stripEmojis } from "../../utils/entities";

const TONE_CLASS = {
  LOW: style.toneLow,
  MEDIUM: style.toneMedium,
  HIGH: style.toneHigh,
};

// Acima disso a mensagem começa recolhida
const COLLAPSE_LINES = 8;
const COLLAPSE_CHARS = 600;

function channelInfo(meta) {
  const username = meta?.channel_username?.replace(/^@+/, "");
  return {
    label: username ? `@${username}` : meta?.channel_name ?? null,
    url: username ? `https://t.me/${username}` : null,
  };
}

function CopyButton({ value, label = "Copiar", className, showLabel = false }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Sem permissão de clipboard: não há o que fazer além de não confirmar
    }
  };

  return (
    <button type="button" className={className} onClick={copy} title={label} aria-label={label}>
      {copied ? <PiCheck aria-hidden="true" /> : <PiCopy aria-hidden="true" />}
      {showLabel && (copied ? "Copiado" : label)}
    </button>
  );
}

function Section({ title, children }) {
  return (
    <section className={style.section}>
      <h3 className={style.sectionTitle}>{title}</h3>
      {children}
    </section>
  );
}

function OriginalMessage({ text }) {
  const [expanded, setExpanded] = useState(false);
  const long = text.split("\n").length > COLLAPSE_LINES || text.length > COLLAPSE_CHARS;

  return (
    <>
      <blockquote className={style.message}>
        {/* O corte fica num elemento interno para não vazar no padding da citação */}
        <div className={long && !expanded ? style.messageCollapsed : undefined}>{text || "Sem conteúdo"}</div>
      </blockquote>
      {long && (
        <button type="button" className={style.toggle} onClick={() => setExpanded((e) => !e)}>
          {expanded ? "Mostrar menos" : "Mostrar tudo"}
        </button>
      )}
    </>
  );
}

export default function PostDrawer({ post, onClose, onPrev, onNext }) {
  // Navegação por teclado: ↑/↓ ou K/J troca de post, Esc fecha
  useEffect(() => {
    if (!post) return;

    const handleKey = (e) => {
      if (e.target.closest?.("input, textarea, select")) return;
      const key = e.key.toLowerCase();
      if (key === "escape") onClose();
      else if ((key === "arrowdown" || key === "j") && onNext) {
        e.preventDefault();
        onNext();
      } else if ((key === "arrowup" || key === "k") && onPrev) {
        e.preventDefault();
        onPrev();
      }
    };

    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [post, onClose, onPrev, onNext]);

  const text = useMemo(() => stripEmojis(post?.content ?? post?.title ?? ""), [post]);

  if (!post) return null;

  const classification = post.classification;
  const score = classification?.score;
  const level = getRelevanceLevel(score);
  const tone = TONE_CLASS[level ?? "LOW"];
  const channel = channelInfo(post.meta);
  const published = formatDateTime(post.createdAt);
  const classifiedAt = formatDateTime(classification?.classifiedAt);
  const sourceMeta = sourceMetaEntries(post.meta);
  return (
    <aside className={style.drawer} role="dialog" aria-label="Detalhes do post">
      <header className={style.header}>
        <div className={style.headerInfo}>
          {score != null && <ScorePill score={score} showLabel />}
          <span className={style.meta}>{[channel.label, published].filter(Boolean).join(" · ")}</span>
        </div>
        <div className={style.headerActions}>
          <button type="button" onClick={onPrev} disabled={!onPrev} title="Post anterior (↑ ou K)" aria-label="Post anterior">
            <PiArrowUp aria-hidden="true" />
          </button>
          <button type="button" onClick={onNext} disabled={!onNext} title="Próximo post (↓ ou J)" aria-label="Próximo post">
            <PiArrowDown aria-hidden="true" />
          </button>
          <button type="button" onClick={onClose} title="Fechar (Esc)" aria-label="Fechar">
            <PiX aria-hidden="true" />
          </button>
        </div>
      </header>

      <div className={style.scroll}>
        <Section title="Classificação">
          <dl className={style.fields}>
            {score != null ? (
              <>
                <dt>Score</dt>
                <dd className={style.scoreRow}>
                  <span className={style.scoreTrack}>
                    <span className={tone} style={{ width: `${Math.round(score * 100)}%` }} />
                  </span>
                  <strong>{score.toFixed(2)}</strong>
                  <span className={style.hint}>
                    {RELEVANCE_LEVELS[level].label} · {RELEVANCE_LEVELS[level].range}
                  </span>
                </dd>
              </>
            ) : (
              <>
                <dt>Status</dt>
                <dd>Ainda não classificado</dd>
              </>
            )}
            {classification?.ioc != null && (
              <>
                <dt>IoC</dt>
                <dd>{classification.ioc ? "Sim" : "Não"}</dd>
              </>
            )}
            {post.category && (
              <>
                <dt>Categoria</dt>
                <dd>{post.category}</dd>
              </>
            )}
            {classifiedAt && (
              <>
                <dt>Classificado</dt>
                <dd>{classifiedAt}</dd>
              </>
            )}
          </dl>
        </Section>

        <Section title="Fonte">
          <dl className={style.fields}>
            <dt>Origem</dt>
            <dd>{formatSource(post.source)}</dd>
            {published && (
              <>
                <dt>Publicado</dt>
                <dd>{published}</dd>
              </>
            )}
            {sourceMeta.map(({ key, label, value }) => (
              <div key={key} className={style.fieldRow}>
                <dt>{label}</dt>
                <dd>
                  {key === "channel_username" && channel.url ? (
                    <a href={channel.url} target="_blank" rel="noopener noreferrer">
                      {channel.label} <PiArrowSquareOut aria-hidden="true" />
                    </a>
                  ) : (
                    value
                  )}
                </dd>
              </div>
            ))}
          </dl>
        </Section>

        <Section title="Mensagem original">
          <OriginalMessage key={post.id} text={text} />
        </Section>
      </div>

      <footer className={style.footer}>
        {channel.url && (
          <a className={style.action} href={channel.url} target="_blank" rel="noopener noreferrer">
            <PiTelegramLogo aria-hidden="true" /> Abrir canal no Telegram
          </a>
        )}
        <CopyButton value={text} label="Copiar texto" className={style.action} showLabel />
        <span className={style.postId}>ID {post.id}</span>
      </footer>
    </aside>
  );
}
