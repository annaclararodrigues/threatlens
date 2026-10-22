import { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { PiCaretLeft, PiCaretRight, PiMagnifyingGlass, PiX } from "react-icons/pi";
import style from "./Posts.module.css";
import Header from "../../components/Header/Header";
import AllPostsTable from "../../components/AllPostsTable/AllPostsTable";
import PeriodPicker from "../../components/PeriodPicker/PeriodPicker";
import PostDrawer from "../../components/PostDrawer/PostDrawer";
import SourceFilter from "../../components/SourceFilter/SourceFilter";
import { formatSource } from "../../utils/sources";
import { usePosts } from "../../hooks";
import { RELEVANCE_LEVELS } from "../../utils/relevance";
import {
  RELEVANCE_VALUES,
  DEFAULT_ORDER,
  SORT_FIELDS,
  isAllTime,
  periodLabel,
  periodToQuery,
  readPeriod,
  readSources,
  writePeriod,
  writeSources,
} from "../../utils/filters";

const SEARCH_DEBOUNCE_MS = 300;
const PAGE_SIZE = 50;

const RELEVANCE_TONE = {
  HIGH: style.toneHigh,
  MEDIUM: style.toneMedium,
  LOW: style.toneLow,
};

export default function Posts() {
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedId = searchParams.get("id");
  // O post aberto (id) não é filtro: trocar de post não pode resetar a página
  const filterKey = useMemo(() => {
    const params = new URLSearchParams(searchParams);
    params.delete("id");
    return params.toString();
  }, [searchParams]);

  // Filtros vêm da URL (assim os links do dashboard abrem a lista já filtrada)
  const filters = useMemo(() => {
    const relevance = searchParams.get("relevance");
    const sortParam = searchParams.get("sort");
    const orderParam = searchParams.get("order");
    const sort = SORT_FIELDS.includes(sortParam) ? sortParam : SORT_FIELDS[0];
    const order = orderParam === "ASC" || orderParam === "DESC" ? orderParam : DEFAULT_ORDER[sort];
    return {
      period: readPeriod(searchParams),
      relevance: RELEVANCE_VALUES.includes(relevance) ? relevance : null,
      sources: readSources(searchParams),
      search: searchParams.get("search") ?? "",
      sort,
      order,
    };
  }, [filterKey]); // eslint-disable-line react-hooks/exhaustive-deps

  // Página volta para 0 sempre que um filtro muda
  const [pageState, setPageState] = useState({ key: filterKey, page: 0 });
  const page = pageState.key === filterKey ? pageState.page : 0;
  const setPage = (next) => {
    setPageState({ key: filterKey, page: next });
    // Trocar de página também fecha o post aberto, pelo mesmo motivo dos filtros
    if (selectedId) setParam("id", null, { replace: true });
  };

  const { period, from, to } = periodToQuery(filters.period);
  const { posts, pagination, loading } = usePosts({
    page,
    size: PAGE_SIZE,
    period,
    from,
    to,
    relevance: filters.relevance ?? undefined,
    sources: filters.sources,
    search: filters.search || undefined,
    sort: filters.sort,
    order: filters.order,
  });

  // O "prev" do setSearchParams funcional é o do render em que a função foi criada;
  // callbacks guardados (debounce, drawer) usariam parâmetros velhos e apagariam filtros.
  // Por isso toda atualização parte da URL atual, via ref.
  const latestParams = useRef(searchParams);
  latestParams.current = searchParams;

  const withoutId = (params) => {
    const copy = new URLSearchParams(params);
    copy.delete("id");
    copy.sort();
    return copy.toString();
  };

  const updateParams = (mutate, options) => {
    const params = new URLSearchParams(latestParams.current);
    mutate(params);
    // Mudou algum filtro? O post aberto pode não estar mais na lista: fecha o drawer
    // (senão o id fica na URL e o post reabre sozinho ao voltar para o filtro anterior)
    if (withoutId(params) !== withoutId(latestParams.current)) params.delete("id");
    latestParams.current = params;
    setSearchParams(params, options);
  };

  const setParam = (key, value, options) =>
    updateParams((params) => (value ? params.set(key, value) : params.delete(key)), options);

  // Drawer: post selecionado vem da URL (?id=), navegação dentro da página atual
  const selectedIndex = posts.findIndex((p) => String(p.id) === selectedId);
  const selectedPost = selectedIndex >= 0 ? posts[selectedIndex] : null;
  const selectPost = (post) => setParam("id", post ? String(post.id) : null, { replace: true });
  const closeDrawer = () => selectPost(null);
  const prevPost = selectedIndex > 0 ? () => selectPost(posts[selectedIndex - 1]) : null;
  const nextPost = selectedIndex >= 0 && selectedIndex < posts.length - 1 ? () => selectPost(posts[selectedIndex + 1]) : null;

  // Busca com debounce; o input só é sobrescrito quando a URL muda por outro motivo (ex.: "Limpar tudo")
  const [searchInput, setSearchInput] = useState(filters.search);
  const lastWrittenSearch = useRef(filters.search);

  useEffect(() => {
    if (filters.search !== lastWrittenSearch.current) {
      lastWrittenSearch.current = filters.search;
      setSearchInput(filters.search);
    }
  }, [filters.search]);

  useEffect(() => {
    const value = searchInput.trim();
    if (value === lastWrittenSearch.current) return;
    const timeout = setTimeout(() => {
      lastWrittenSearch.current = value;
      setParam("search", value, { replace: true });
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timeout);
  }, [searchInput]); // eslint-disable-line react-hooks/exhaustive-deps

  // Clique no cabeçalho: mesma coluna inverte a ordem; outra coluna começa na ordem padrão dela
  const handleSort = (field) => {
    const order = filters.sort === field
      ? (filters.order === "ASC" ? "DESC" : "ASC")
      : DEFAULT_ORDER[field];
    updateParams((params) => {
      params.set("sort", field);
      params.set("order", order);
    });
  };

  const handlePeriodChange = (value) => updateParams((params) => writePeriod(params, value));
  const handleSourcesChange = (sources) => updateParams((params) => writeSources(params, sources));

  const clearAll = () => {
    lastWrittenSearch.current = "";
    setSearchInput("");
    updateParams((params) => [...params.keys()].forEach((key) => params.delete(key)));
  };

  const activeFilters = [
    filters.relevance && {
      key: "relevance",
      label: `${RELEVANCE_LEVELS[filters.relevance].label} relevância`,
      onRemove: () => setParam("relevance", null),
    },
    filters.sources.length > 0 && {
      key: "source",
      label: `Fonte: ${filters.sources.map(formatSource).join(", ")}`,
      onRemove: () => handleSourcesChange([]),
    },
    !isAllTime(filters.period) && {
      key: "period",
      label: periodLabel(filters.period),
      onRemove: () => handlePeriodChange({ period: "ALL" }),
    },
    filters.search && {
      key: "search",
      label: `"${filters.search}"`,
      onRemove: () => setParam("search", null),
    },
  ].filter(Boolean);

  return (
    <div className={selectedPost ? style.withDrawer : undefined}>
      <Header pageName="Posts" pageDescription="Analise os posts inseridos no sistema." />

      <div className={`${style.filterBar} ${selectedPost ? style.filterBarCompact : ""}`}>
        <label className={style.search}>
          <PiMagnifyingGlass aria-hidden="true" />
          <input
            type="search"
            placeholder="Buscar no conteúdo..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            aria-label="Buscar no conteúdo dos posts"
          />
        </label>

        <div className={style.segmented} role="radiogroup" aria-label="Relevância">
          {[null, ...RELEVANCE_VALUES].map((level) => {
            const selected = filters.relevance === level;
            return (
              <button
                key={level ?? "ALL"}
                type="button"
                role="radio"
                aria-checked={selected}
                className={`${style.segment} ${level ? RELEVANCE_TONE[level] : ""} ${selected ? style.segmentSelected : ""}`}
                title={level ? RELEVANCE_LEVELS[level].range : "Todas as relevâncias"}
                onClick={() => setParam("relevance", level)}
              >
                {level ? RELEVANCE_LEVELS[level].label : "Todas"}
              </button>
            );
          })}
        </div>

        <SourceFilter value={filters.sources} onChange={handleSourcesChange} />

        <PeriodPicker value={filters.period} onChange={handlePeriodChange} align="left" />
      </div>

      <div className={style.summary}>
        {pagination && (
          <span className={style.count}>
            {pagination.total.toLocaleString("pt-BR")} {pagination.total === 1 ? "resultado" : "resultados"}
          </span>
        )}
        {activeFilters.map((f) => (
          <span key={f.key} className={style.activeChip}>
            {f.label}
            <button type="button" onClick={f.onRemove} aria-label={`Remover filtro ${f.label}`}>
              <PiX aria-hidden="true" />
            </button>
          </span>
        ))}
        {activeFilters.length > 0 && (
          <button type="button" className={style.clearAll} onClick={clearAll}>Limpar</button>
        )}
      </div>

      <div className={style.postsContainer}>
        <AllPostsTable
          posts={posts}
          loading={loading}
          selectedId={selectedId}
          onSelect={selectPost}
          compact={Boolean(selectedPost)}
          sort={filters.sort}
          order={filters.order}
          onSort={handleSort}
        />

        {pagination && pagination.total > 0 && (
          <div className={style.pagination}>
            <span className={style.pageInfo}>
              {(page * PAGE_SIZE + 1).toLocaleString("pt-BR")}–{Math.min((page + 1) * PAGE_SIZE, pagination.total).toLocaleString("pt-BR")} de {pagination.total.toLocaleString("pt-BR")}
            </span>
            <button
              type="button"
              className={style.pageBtn}
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              aria-label="Página anterior"
            >
              <PiCaretLeft aria-hidden="true" />
            </button>
            <button
              type="button"
              className={style.pageBtn}
              disabled={page >= pagination.totalPages - 1}
              onClick={() => setPage(page + 1)}
              aria-label="Próxima página"
            >
              <PiCaretRight aria-hidden="true" />
            </button>
          </div>
        )}
      </div>

      <PostDrawer
        post={selectedPost}
        onClose={closeDrawer}
        onPrev={prevPost}
        onNext={nextPost}
      />
    </div>
  );
}
