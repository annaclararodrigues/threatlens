import { useState, useEffect, useCallback } from "react";
import { getWordCloud } from "../services/post/postService";

export function useWordCloud({ period, from, to, sources, limit } = {}) {
  const [words, setWords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const sourcesKey = sources?.join(",") ?? "";
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = {};
    if (from || to) {
      if (from) params.from = from;
      if (to) params.to = to;
    } else if (period && period !== "ALL") {
      params.period = period;
    }
    // Lista separada por vírgula: o axios serializaria arrays como "sources[]=", que o Spring não lê
    if (sourcesKey) params.sources = sourcesKey;
    if (limit) params.limit = limit;

    getWordCloud(params)
      .then((data) => {
        if (!cancelled) {
          setWords((data?.words ?? []).map((w) => ({ text: w.word, value: w.count })));
        }
      })
      .catch((err) => { if (!cancelled) setError(err); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [period, from, to, sourcesKey, limit, reloadKey]);

  return { words, loading, error, reload };
}
