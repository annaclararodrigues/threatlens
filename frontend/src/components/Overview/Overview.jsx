import { useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import style from "./Overview.module.css";
import ProportionCard from "../ProportionCard/ProportionCard";
import RelevantPostsTable from "../RelevantPostsTable/RelevantPostsTable";
import BarChart from "../BarChart/BarChart";
import DoughnutChart from "../DoughnutChart/DoughnutChart";
import WordCloud from "../WordCloud/WordCloud";
import Header from "../Header/Header";
import FilterBar from "../FilterBar/FilterBar";
import DashboardState from "../DashboardState/DashboardState";
import DashboardGreeting from "../DashboardGreeting/DashboardGreeting";
import OverviewSkeleton from "../OverviewSkeleton/OverviewSkeleton";
import { PiDatabase, PiMagnifyingGlass, PiWarningCircle } from "react-icons/pi";
import { useStats, usePosts, useWordCloud } from "../../hooks";
import {
  isAllTime,
  periodPhrase,
  periodToQuery,
  postsLink,
  previousWindow as getPreviousWindow,
  readPeriod,
  readSources,
  writePeriod,
  writeSources,
} from "../../utils/filters";

// Busca palavras a mais porque a nuvem descarta ruído (URLs etc.) antes de exibir as 30 primeiras
const WORD_CLOUD_FETCH_LIMIT = 60;

const TOP_POSTS_COUNT = 5;

export default function Overview() {
  const [searchParams, setSearchParams] = useSearchParams();
  const periodKey = searchParams.toString();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const periodValue = useMemo(() => readPeriod(searchParams), [periodKey]);
  const { period, from, to } = periodToQuery(periodValue);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const sources = useMemo(() => readSources(searchParams), [periodKey]);

  const previousWindow = useMemo(() => getPreviousWindow(periodValue), [periodValue]);

  const { stats, loading: statsLoading, error: statsError, reload: reloadStats } = useStats({ period, from, to, sources });
  const { stats: previousStats } = useStats({
    from: previousWindow?.from,
    to: previousWindow?.to,
    sources,
    skip: !previousWindow,
  });
  const { posts: topPosts, error: postsError, reload: reloadPosts } = usePosts({ sort: "SCORE", order: "DESC", size: TOP_POSTS_COUNT, period, from, to, sources });
  const { words: wordCloudData, error: wordCloudError, reload: reloadWordCloud } = useWordCloud({ period, from, to, sources, limit: WORD_CLOUD_FETCH_LIMIT });

  const analyzedPosts = stats?.summary.totalPosts ?? 0;
  const relevantPosts = stats?.summary.relevantPostsInPeriod ?? 0;
  const phrase = periodPhrase(periodValue);
  const linkTo = (filters) => postsLink(periodValue, { sources, ...filters });

  const updateParams = (write) => {
    setSearchParams((prev) => {
      const params = new URLSearchParams(prev);
      write(params);
      return params;
    });
  };
  const handlePeriodChange = (value) => updateParams((params) => writePeriod(params, value));
  const handleSourcesChange = (value) => updateParams((params) => writeSources(params, value));

  const handleRetry = () => {
    reloadStats();
    reloadPosts();
    reloadWordCloud();
  };

  // Erro tem prioridade: sem ele, uma falha da API apareceria como "0 posts"
  let status = "ready";
  if (statsError) status = "error";
  else if (!stats) status = "loading";
  else if (analyzedPosts === 0) status = "empty";

  const renderState = () => {
    if (status === "error") {
      return (
        <DashboardState
          tone="error"
          icon={<PiWarningCircle />}
          title="Não foi possível carregar os dados"
          description="O servidor não respondeu como esperado. Os números não foram exibidos para não mostrar informação incorreta."
          actionLabel="Tentar novamente"
          onAction={handleRetry}
        />
      );
    }
    if (status === "loading") return <OverviewSkeleton />;
    if (isAllTime(periodValue)) {
      return (
        <DashboardState
          icon={<PiDatabase />}
          title="Nenhum post coletado ainda"
          description="Assim que a coleta do Telegram trouxer mensagens, elas aparecem aqui. Se isso não acontecer, verifique se o coletor está ativo."
        />
      );
    }
    return (
      <DashboardState
        icon={<PiMagnifyingGlass />}
        title={`Nenhum post ${phrase}`}
        description="Não há posts coletados para o período selecionado. Tente um intervalo maior."
        actionLabel="Ver todo o período"
        onAction={() => handlePeriodChange({ period: "ALL" })}
      />
    );
  };

  return (
    <div>
      <Header pageName="Dashboard" pageDescription={<DashboardGreeting />} />
      <FilterBar
        value={periodValue}
        onChange={handlePeriodChange}
        sources={sources}
        onSourcesChange={handleSourcesChange}
      />
      {status !== "ready" ? (
        renderState()
      ) : (
        <div className={statsLoading ? style.refreshing : undefined}>
          <div className={style.infosContainer}>
            <div className={style.infosContainerCol1}>
              <ProportionCard
                analyzedPosts={analyzedPosts}
                relevantPosts={relevantPosts}
                label={phrase}
                sourceCount={stats?.sources.length ?? 0}
                comparisonLabel={previousWindow?.label}
                previous={previousWindow && previousStats ? {
                  analyzedPosts: previousStats.summary.totalPosts,
                  relevantPosts: previousStats.summary.relevantPostsInPeriod,
                } : null}
                analyzedLink={linkTo()}
                relevantLink={linkTo({ relevance: "HIGH" })}
              />
              <RelevantPostsTable
                mostRelevantPosts={topPosts}
                error={postsError}
                seeAllLink={linkTo({ relevance: "HIGH", sort: "SCORE", order: "DESC" })}
              />
            </div>
            <div className={style.infosContainerCol2}>
              <DoughnutChart
                low={stats?.relevanceDistribution.lowCount ?? 0}
                medium={stats?.relevanceDistribution.mediumCount ?? 0}
                high={stats?.relevanceDistribution.highCount ?? 0}
                linkForLevel={(relevance) => linkTo({ relevance })}
              />
            </div>
          </div>
          <div className={style.bottomRow}>
            <BarChart
              sources={stats?.sources ?? []}
              label={phrase}
              linkForSource={(source) => linkTo({ source })}
            />
            <WordCloud
              palavras={wordCloudData}
              error={wordCloudError}
              linkForWord={(word) => linkTo({ search: word })}
            />
          </div>
        </div>
      )}
    </div>
  );
}
