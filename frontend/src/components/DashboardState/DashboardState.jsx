import style from "./DashboardState.module.css";

export default function DashboardState({ icon, title, description, actionLabel, onAction, tone = "empty" }) {
  return (
    <div className={`${style.state} ${tone === "error" ? style.error : ""}`} role={tone === "error" ? "alert" : "status"}>
      <span className={style.icon} aria-hidden="true">{icon}</span>
      <h2 className={style.title}>{title}</h2>
      {description && <p className={style.description}>{description}</p>}
      {actionLabel && (
        <button type="button" className={style.action} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
