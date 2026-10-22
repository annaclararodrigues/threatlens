import { useState, useEffect, useCallback } from "react";
import { getPosts } from "../services/post/postService"

export function usePosts({ page = 0, size = 20, period, from, to, relevance, sources, sort, order, category, search } = {}) {
  const [posts, setPosts] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const sourcesKey = sources?.join(",") ?? "";
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = { page, size };
    if (from || to) {
      if (from) params.from = from;
      if (to) params.to = to;
    } else if (period && period !== "ALL") {
      params.period = period;
    }
    if (relevance) params.relevance = relevance;
    // Lista separada por vírgula: o axios serializaria arrays como "sources[]=", que o Spring não lê
    if (sources?.length) params.sources = sources.join(",");
    if (sort) params.sort = sort;
    if (order) params.order = order;
    if (category) params.category = category;
    if (search) params.search = search;

    getPosts(params)
      .then((result) => {
        if (!cancelled) {
          setPosts(result.data);
          setPagination(result.pagination);
        }
      })
      .catch((err) => { if (!cancelled) setError(err); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [page, size, period, from, to, relevance, sourcesKey, sort, order, category, search, reloadKey]); // eslint-disable-line react-hooks/exhaustive-deps

  return { posts, pagination, loading, error, reload };
}
