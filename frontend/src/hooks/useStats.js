import { useState, useEffect, useCallback } from "react";
import { getStats } from "../services/post/postService";

export function useStats({ period, from, to, sources, skip = false } = {}) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const sourcesKey = sources?.join(",") ?? "";
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    if (skip) {
      setStats(null);
      return;
    }

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

    getStats(params)
      .then((data) => { if (!cancelled) setStats(data); })
      .catch((err) => { if (!cancelled) setError(err); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [period, from, to, sourcesKey, skip, reloadKey]);

  return { stats, loading, error, reload };
}
