import style from "./ProportionCard.module.css";
import { Link } from "react-router-dom";
import { PiFileText, PiCrosshair } from "react-icons/pi";

function Delta({ current, previous, label }) {
    if (!label || !previous) return null;

    const change = ((current - previous) / previous) * 100;
    const arrow = change > 0 ? "▲" : change < 0 ? "▼" : "■";

    return (
        <p className={style.footer}>
            <span className={style.deltaValue}>{arrow} {Math.abs(change).toFixed(0)}%</span> vs. {label}
        </p>
    );
}

export default function ProportionCard(props) {
    const relevantPct = props.analyzedPosts
        ? (props.relevantPosts / props.analyzedPosts * 100).toFixed(1)
        : "0.0";

    return (
        <div className={style.proportionCardsContainer}>
            <Link to={props.analyzedLink} className={style.proportionCard} title="Ver todos os posts do período">
                <div className={style.cardHeader}>
                    <span className={style.iconChip}><PiFileText /></span>
                    <h3 className={style.description}>Posts analisados {props.label}</h3>
                </div>
                <h2 className={style.value}>{props.analyzedPosts.toLocaleString("pt-BR")}</h2>
                {props.comparisonLabel && props.previous?.analyzedPosts ? (
                    <Delta current={props.analyzedPosts} previous={props.previous.analyzedPosts} label={props.comparisonLabel} />
                ) : (
                    <p className={style.footer}>
                        em {props.sourceCount} {props.sourceCount === 1 ? "fonte monitorada" : "fontes monitoradas"}
                    </p>
                )}
            </Link>
            <Link
                to={props.relevantLink}
                className={`${style.proportionCard} ${style.highlight}`}
                title="Posts de alta relevância (score ≥ 0.70)"
            >
                <div className={style.cardHeader}>
                    <span className={style.iconChip}><PiCrosshair /></span>
                    <h3 className={style.description}>Posts relevantes {props.label}</h3>
                </div>
                <div className={style.valueContainer}>
                    <h2 className={style.value}>{props.relevantPosts.toLocaleString("pt-BR")}</h2>
                    <span className={style.percentValue}>{relevantPct}% do total</span>
                </div>
                <div className={style.progressTrack}>
                    <div className={style.progressFill} style={{ width: `${Math.min(relevantPct, 100)}%` }} />
                </div>
                <Delta current={props.relevantPosts} previous={props.previous?.relevantPosts} label={props.comparisonLabel} />
            </Link>
        </div>
    )
}
