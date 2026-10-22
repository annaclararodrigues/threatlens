import style from "./Sidebar.module.css";
import { MdClose } from "react-icons/md";
import Navigation from "../Navigation/Navigation";
import logoDark from "../../assets/brand/logo-dark.png";
import logoLight from "../../assets/brand/logo-light.png";

export default function Sidebar({ isOpen, onClose }) {
  return (
    <aside className={`${style.sidebar} ${isOpen ? style.open : ""}`}>
      <button className={style.closeBtn} onClick={onClose} aria-label="Fechar menu">
        <MdClose />
      </button>
      {/* As duas versões ficam no HTML; o CSS mostra a do tema ativo (texto claro no escuro e vice-versa) */}
      <h1 className={style.title}>
        <img src={logoDark} alt="ThreatLens" className={`${style.logo} ${style.logoDark}`} />
        <img src={logoLight} alt="ThreatLens" className={`${style.logo} ${style.logoLight}`} />
      </h1>
      <Navigation />
    </aside>
  );
}
