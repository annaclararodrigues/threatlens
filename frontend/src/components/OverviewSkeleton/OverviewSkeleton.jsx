import style from "./OverviewSkeleton.module.css";
import overviewStyle from "../Overview/Overview.module.css";

export default function OverviewSkeleton() {
  return (
    <div aria-busy="true" aria-label="Carregando dados do dashboard">
      <div className={overviewStyle.infosContainer}>
        <div className={overviewStyle.infosContainerCol1}>
          <div className={style.kpiRow}>
            <div className={`${style.block} ${style.kpi}`} />
            <div className={`${style.block} ${style.kpi}`} />
          </div>
          <div className={`${style.block} ${style.list}`} />
        </div>
        <div className={overviewStyle.infosContainerCol2}>
          <div className={`${style.block} ${style.donut}`} />
        </div>
      </div>
      <div className={overviewStyle.bottomRow}>
        <div className={style.block} />
        <div className={style.block} />
      </div>
    </div>
  );
}
