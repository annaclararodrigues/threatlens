import { useState, useEffect, useCallback, useRef } from "react";
import { getPosts } from "../services/post/postService"

// withTotal=false: quem não exibe o total (ex.: top posts do dashboard) não paga a contagem
export function usePosts({ page = 0, size = 20, period, from, to, relevance, sources, sort, order, category, search, withTotal = true } = {}) {
  const [posts, setPosts] = useState([]);
  const [pagination, setPagination] = useState(null);
  // Começa carregando: a busca sai no primeiro efeito
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const sourcesKey = sources?.join(",") ?? "";
  const reload = useCallback(() => setReloadKey((k) => k + 1), []);
  // Total da última contagem e os filtros a que ele pertence: trocar só de página reaproveita
  const totalRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = { size };
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

    // Tudo menos a página: se isso não mudou, o total também não mudou ("Recarregar" reconta)
    const totalKey = `${JSON.stringify(params)}|${reloadKey}`;
    const knownTotal = totalRef.current?.key === totalKey ? totalRef.current : null;
    if (!withTotal || knownTotal) params.includeTotal = false;
    params.page = page;

    getPosts(params)
      .then((result) => {
        if (cancelled) return;
        let next = result.pagination;
        if (next?.total != null) {
          totalRef.current = { key: totalKey, total: next.total, totalPages: next.totalPages };
        } else if (knownTotal) {
          next = { ...next, total: knownTotal.total, totalPages: knownTotal.totalPages };
        }
        setPosts(result.data);
        setPagination(next);
      })
      .catch((err) => { if (!cancelled) setError(err); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [page, size, period, from, to, relevance, sourcesKey, sort, order, category, search, withTotal, reloadKey]); // eslint-disable-line react-hooks/exhaustive-deps

  return { posts, pagination, loading, error, reload };
}
