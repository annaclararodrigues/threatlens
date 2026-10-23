import { useCallback, useEffect, useState } from "react";
import { getPostReplies } from "../services/post/postService";

// Fontes com respostas e o que são os itens abaixo do post: respostas (com comentários) ou,
// no Raddle, comentários feitos direto no post. O backend confirma o tipo em "kind".
const REPLY_SOURCES = {
  HA: "ANSWER",
  DEEP: "ANSWER",
  NULLEDBB: "ANSWER",
  NIFLHEIM: "ANSWER",
  HACKONOLOGY: "ANSWER",
  LEGITCARDERS: "ANSWER",
  RADDLE: "COMMENT",
};

export function hasReplies(post) {
  return Boolean(REPLY_SOURCES[post?.source]);
}

// Respostas do post aberto; só busca quando a fonte tem respostas
export function usePostReplies(post) {
  const postId = hasReplies(post) ? post.id : null;
  // Guarda de qual post é o resultado, para não mostrar as respostas do anterior ao trocar
  const [result, setResult] = useState({ postId: null, kind: null, answers: [], error: null });
  const [reloadKey, setReloadKey] = useState(0);
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    if (!postId) return;

    let cancelled = false;
    setResult({ postId: null, kind: null, answers: [], error: null });
    getPostReplies(postId)
      .then((data) => { if (!cancelled) setResult({ postId, kind: data.kind, answers: data.answers ?? [], error: null }); })
      .catch((err) => { if (!cancelled) setResult({ postId, kind: null, answers: [], error: err }); });

    return () => { cancelled = true; };
  }, [postId, reloadKey]);

  const ready = postId != null && result.postId === postId;
  return {
    kind: (ready && result.kind) || REPLY_SOURCES[post?.source] || "ANSWER",
    answers: ready ? result.answers : [],
    loading: postId != null && !ready,
    error: ready ? result.error : null,
    reload,
  };
}
