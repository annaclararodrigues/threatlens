import { PiTelegramLogo, PiGlobe } from "react-icons/pi";
import style from "./SourceBadge.module.css";

// Ícone e cor de marca por fonte; fontes sem ícone próprio usam um globo neutro
const BADGES = {
  TELEGRAM: { Icon: PiTelegramLogo, className: style.telegram },
};

export default function SourceBadge({ source, size = 22 }) {
  const { Icon, className } = BADGES[source?.toUpperCase()] ?? { Icon: PiGlobe, className: style.generic };
  return (
    <span
      className={`${style.badge} ${className}`}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.6) }}
      aria-hidden="true"
    >
      <Icon />
    </span>
  );
}
