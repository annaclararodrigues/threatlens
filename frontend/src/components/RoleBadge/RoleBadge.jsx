import style from "./RoleBadge.module.css";
import { roleLabel } from "../../utils/user";

export default function RoleBadge({ role }) {
  return (
    <span className={`${style.badge} ${role === "ADMIN" ? style.admin : ""}`}>
      {roleLabel(role)}
    </span>
  );
}
