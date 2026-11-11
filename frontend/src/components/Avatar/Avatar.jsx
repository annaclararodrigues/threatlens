import style from "./Avatar.module.css";
import { initials } from "../../utils/user";

// Avatar com as iniciais do nome: dá identidade sem exigir upload de foto
export default function Avatar({ name, size = "md" }) {
  return (
    <span className={`${style.avatar} ${style[size]}`} aria-hidden="true">
      {initials(name)}
    </span>
  );
}
