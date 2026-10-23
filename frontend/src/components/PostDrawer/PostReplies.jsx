import { useState } from "react";
import { PiArrowBendDownRight, PiArrowFatUp, PiChatCircle, PiThumbsDown, PiThumbsUp } from "react-icons/pi";
import style from "./PostReplies.module.css";
import ExpandableText from "../ExpandableText/ExpandableText";
import { formatDateTime } from "../../utils/format";
import { stripEmojis } from "../../utils/entities";

// Listas longas abrem parciais
const ANSWERS_PAGE = 5;
const COMMENTS_PAGE = 3;

function Byline({ author, createdAt }) {
  const date = formatDateTime(createdAt);
  return (
    <span className={style.byline}>
      <strong>{author || "Anônimo"}</strong>
      {date && <> · {date}</>}
    </span>
  );
}

function Votes({ meta }) {
  // HA separa votos positivos e negativos; Deep tem um saldo único
  const up = meta?.up_votes;
  const down = meta?.down_votes;
  const votes = meta?.votes;
  const points = meta?.points;
  if (up == null && down == null && votes == null && points == null) return null;

  return (
    <span className={style.votes}>
      {votes != null && (
        <span title="Votos"><PiArrowFatUp aria-hidden="true" /> {votes}</span>
      )}
      {up != null && (
        <span title="Votos positivos"><PiThumbsUp aria-hidden="true" /> {up}</span>
      )}
      {down != null && (
        <span title="Votos negativos"><PiThumbsDown aria-hidden="true" /> {down}</span>
      )}
      {points != null && <span title="Pontos">{points} pts</span>}
    </span>
  );
}

function ShowMore({ hidden, noun, onClick }) {
  if (hidden <= 0) return null;
  return (
    <button type="button" className={style.more} onClick={onClick}>
      Mostrar mais {hidden} {noun}
    </button>
  );
}

function Comments({ comments }) {
  const [visible, setVisible] = useState(COMMENTS_PAGE);
  const count = comments.length;

  return (
    <div className={style.comments}>
      <h5 className={style.commentsTitle}>
        <PiChatCircle aria-hidden="true" /> {count} {count === 1 ? "comentário" : "comentários"} nesta resposta
      </h5>
      <ul className={style.commentList}>
        {comments.slice(0, visible).map((comment) => (
          <li key={comment.id} className={style.comment}>
            <div className={style.commentHeader}>
              <PiArrowBendDownRight aria-hidden="true" className={style.commentIcon} />
              <span className={`${style.kind} ${style.kindComment}`}>Comentário</span>
              <Byline author={comment.author} createdAt={comment.createdAt} />
            </div>
            <ExpandableText className={style.commentText} text={stripEmojis(comment.content ?? "")} lines={4} chars={300} />
          </li>
        ))}
      </ul>
      <ShowMore
        hidden={count - visible}
        noun={count - visible === 1 ? "comentário" : "comentários"}
        onClick={() => setVisible(count)}
      />
    </div>
  );
}

// Textos de cada tipo de item no primeiro nível (ver usePostReplies)
const REPLY_LABELS = {
  ANSWER: { item: "Resposta", one: "resposta", many: "respostas", empty: "Nenhuma resposta coletada para este post." },
  COMMENT: { item: "Comentário", one: "comentário", many: "comentários", empty: "Nenhum comentário coletado para este post." },
};

function Answer({ answer, index, kind }) {
  const comments = answer.comments ?? [];
  const isComment = kind === "COMMENT";
  return (
    <li className={style.answer}>
      <header className={style.answerHeader}>
        <span className={`${style.kind} ${isComment ? style.kindComment : style.kindAnswer}`}>
          {REPLY_LABELS[kind].item} {index + 1}
        </span>
        <Byline author={answer.author} createdAt={answer.createdAt} />
        <Votes meta={answer.meta} />
      </header>
      <ExpandableText className={style.answerText} text={stripEmojis(answer.content ?? "")} lines={6} chars={500} />
      {comments.length > 0 && <Comments comments={comments} />}
    </li>
  );
}

// Respostas ao post, cada uma com os seus comentários logo abaixo (ou, no Raddle, os comentários do post)
export default function PostReplies({ kind = "ANSWER", answers, loading, error, onRetry }) {
  const [visible, setVisible] = useState(ANSWERS_PAGE);
  const labels = REPLY_LABELS[kind];

  if (loading) return <p className={style.status}>Carregando {labels.many}…</p>;
  if (error) {
    return (
      <p className={style.status}>
        Não foi possível carregar os dados.{" "}
        <button type="button" className={style.more} onClick={onRetry}>Tentar de novo</button>
      </p>
    );
  }
  if (answers.length === 0) return <p className={style.status}>{labels.empty}</p>;

  const hidden = answers.length - visible;
  return (
    <>
      <ol className={style.answers}>
        {answers.slice(0, visible).map((answer, index) => (
          <Answer key={answer.id} answer={answer} index={index} kind={kind} />
        ))}
      </ol>
      <ShowMore
        hidden={hidden}
        noun={hidden === 1 ? labels.one : labels.many}
        onClick={() => setVisible((v) => v + ANSWERS_PAGE)}
      />
    </>
  );
}
