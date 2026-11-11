import { useEffect, useMemo, useRef, useState } from "react";
import { PiArrowUp, PiArrowDown, PiCaretDown, PiCaretUp, PiX, PiCopy, PiCheck, PiTelegramLogo, PiArrowSquareOut } from "react-icons/pi";
import style from "./PostDrawer.module.css";
import ScorePill from "../ScorePill/ScorePill";
import ExpandableText from "../ExpandableText/ExpandableText";
import PostReplies from "./PostReplies";
import { hasReplies, usePostReplies } from "../../hooks/usePostReplies";
import { RELEVANCE_LEVELS, formatScore, getRelevanceLevel } from "../../utils/relevance";
import { formatDateTime, formatSource } from "../../utils/format";
import { safeExternalUrl, sourceMetaEntries, splitCategories, stripEmojis } from "../../utils/entities";

const TONE_CLASS = {
  LOW: style.toneLow,
  MEDIUM: style.toneMedium,
  HIGH: style.toneHigh,
};

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

// Metadados que descrevem o próprio post; o resto (canal, fórum, link...) diz de onde ele veio
const POST_META_KEYS = new Set(["language", "tags", "votes", "up_votes", "down_votes", "points"]);

// Detalhes que ficam atrás de "Mostrar todos os detalhes": o resumo fica com o que se usa na triagem
const DETAIL_META_KEYS = new Set([...POST_META_KEYS, "telegram_channel_id", "link"]);

function ChipsField({ label, pluralLabel = label, items }) {
  if (items.length === 0) return null;
  return (
    <div className={`${style.fieldRow} ${style.chipsRow}`}>
      <dt>{items.length > 1 ? pluralLabel : label}</dt>
      <dd>
        <ul className={style.chips}>
          {items.map((item) => (
            <li key={item} className={style.chip}>
              {item}
            </li>
          ))}
        </ul>
      </dd>
    </div>
  );
}

function OriginalMessage({ title, text }) {
  return (
    <>
      {title && <h4 className={style.messageTitle}>{title}</h4>}
      <ExpandableText as="blockquote" className={style.message} text={text} />
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

  const replies = usePostReplies(post);
  // Fica fora do post: quem abriu os detalhes continua vendo-os ao navegar entre posts
  const [showDetails, setShowDetails] = useState(false);

  // Trocar de post (clique na lista ou ↑/↓) volta a leitura para o topo
  const scrollRef = useRef(null);
  useEffect(() => {
    scrollRef.current?.scrollTo(0, 0);
  }, [post?.id]);

  // Título e conteúdo aparecem cada um no seu lugar: conteúdo vazio fica visível como "Sem conteúdo"
  const text = useMemo(() => stripEmojis(post?.content ?? ""), [post]);
  const title = useMemo(() => stripEmojis(post?.title ?? "") || null, [post]);

  if (!post) return null;

  const classification = post.classification;
  const score = classification?.score;
  const level = getRelevanceLevel(score);
  const tone = TONE_CLASS[level ?? "LOW"];
  const channel = channelInfo(post.meta);
  const published = formatDateTime(post.createdAt);
  const classifiedAt = formatDateTime(classification?.classifiedAt);
  const meta = sourceMetaEntries(post.meta);
  const postMeta = meta.filter(({ key }) => POST_META_KEYS.has(key));
  const sourceMeta = meta.filter(({ key }) => !POST_META_KEYS.has(key));
  const hiddenCount = meta.filter(({ key }) => DETAIL_META_KEYS.has(key)).length;
  const repliesLabel = replies.kind === "COMMENT" ? "Comentários" : "Respostas";
  const repliesTitle = replies.loading || replies.error ? repliesLabel : `${repliesLabel} (${replies.answers.length})`;
  const categories = splitCategories(post.category);
  const originalUrl = safeExternalUrl(post.meta?.link);
  const byline = channel.label ?? post.meta?.forum ?? post.author;
  return (
    <aside className={style.drawer} role="dialog" aria-label="Detalhes do post" data-post-drawer>
      <header className={style.header}>
        <div className={style.headerInfo}>
          {score != null && <ScorePill score={score} showLabel />}
          <span className={style.meta}>{[byline, published].filter(Boolean).join(" · ")}</span>
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

      <div className={style.scroll} ref={scrollRef}>
        <Section title="Classificação">
          <dl className={style.fields}>
            {score != null ? (
              <>
                <dt>Score</dt>
                <dd className={style.scoreRow}>
                  <span className={style.scoreTrack}>
                    <span className={tone} style={{ width: `${Math.round(score * 100)}%` }} />
                  </span>
                  <strong>{formatScore(score)}</strong>
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
            {sourceMeta.filter(({ key }) => showDetails || !DETAIL_META_KEYS.has(key)).map(({ key, label, value }) => (
              <div key={key} className={style.fieldRow}>
                <dt>{label}</dt>
                <dd>
                  {key === "channel_username" && channel.url ? (
                    <a href={channel.url} target="_blank" rel="noopener noreferrer">
                      {channel.label} <PiArrowSquareOut aria-hidden="true" />
                    </a>
                  ) : key === "link" && originalUrl ? (
                    <a href={originalUrl} target="_blank" rel="noopener noreferrer">
                      {value} <PiArrowSquareOut aria-hidden="true" />
                    </a>
                  ) : (
                    value
                  )}
                </dd>
              </div>
            ))}
          </dl>
        </Section>

        <Section title="Post">
          <dl className={style.fields}>
            <dt>ID</dt>
            <dd className={style.postId}>
              <span>{post.id}</span>
              <CopyButton value={String(post.id)} label="Copiar ID" className={style.copyInline} />
            </dd>
            {post.author && (
              <>
                <dt>Autor</dt>
                <dd>{post.author}</dd>
              </>
            )}
            {published && (
              <>
                <dt>Publicado</dt>
                <dd>{published}</dd>
              </>
            )}
            <ChipsField label="Categoria" pluralLabel="Categorias" items={categories} />
            {showDetails && postMeta.map(({ key, label, value }) =>
              key === "tags" ? (
                <ChipsField key={key} label="Tags" items={splitCategories(value)} />
              ) : (
                <div key={key} className={style.fieldRow}>
                  <dt>{label}</dt>
                  <dd>{value}</dd>
                </div>
              )
            )}
          </dl>
          {hiddenCount > 0 && (
            <button
              type="button"
              className={style.detailsToggle}
              onClick={() => setShowDetails((s) => !s)}
              aria-expanded={showDetails}
            >
              {showDetails ? <PiCaretUp aria-hidden="true" /> : <PiCaretDown aria-hidden="true" />}
              {showDetails ? "Mostrar menos detalhes" : `Mostrar todos os detalhes (+${hiddenCount})`}
            </button>
          )}
        </Section>

        <Section title="Mensagem original">
          <OriginalMessage key={post.id} title={title} text={text} />
        </Section>

        {hasReplies(post) && (
          <Section title={repliesTitle}>
            <PostReplies key={post.id} {...replies} onRetry={replies.reload} />
          </Section>
        )}
      </div>

      <footer className={style.footer}>
        {channel.url && (
          <a className={style.action} href={channel.url} target="_blank" rel="noopener noreferrer">
            <PiTelegramLogo aria-hidden="true" /> Abrir canal no Telegram
          </a>
        )}
        {originalUrl && (
          <a className={style.action} href={originalUrl} target="_blank" rel="noopener noreferrer">
            <PiArrowSquareOut aria-hidden="true" /> Abrir link original
          </a>
        )}
        <CopyButton value={text || title || ""} label="Copiar texto" className={style.action} showLabel />
      </footer>
    </aside>
  );
}
