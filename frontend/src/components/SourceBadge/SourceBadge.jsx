import { PiTelegramLogo, PiChatsCircle, PiQuestion, PiGlobe } from "react-icons/pi";
import style from "./SourceBadge.module.css";
import { sourceKind } from "../../utils/sources";

// Ícone e cor por tipo de fonte; fontes desconhecidas usam um globo neutro
const BADGES = {
  telegram: { Icon: PiTelegramLogo, className: style.telegram },
  forum: { Icon: PiChatsCircle, className: style.forum },
  qa: { Icon: PiQuestion, className: style.qa },
};

export default function SourceBadge({ source, size = 22 }) {
  const { Icon, className } = BADGES[sourceKind(source)] ?? { Icon: PiGlobe, className: style.generic };
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
