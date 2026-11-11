import style from "./Navigation.module.css";
import NavCategory from "../NavCategory/NavCategory";
import NavItem from "../NavItem/NavItem";
import { AiOutlineHome } from "react-icons/ai";
import { PiChatCenteredDots } from "react-icons/pi";
import { CiUser, CiLogout } from "react-icons/ci";
import { MdAdminPanelSettings } from "react-icons/md";
import { useAuth } from "../../context/AuthContext";
import { useLogoutConfirm } from "../../hooks";

export default function Navigation() {
  const { user } = useAuth();
  const { requestLogout, logoutModal } = useLogoutConfirm();

  return (
    <nav className={style.navigation}>
      <NavCategory title="Geral">
        <NavItem to="/" icon={<AiOutlineHome size={20} className={style.navIcon} />} label="Overview" />
      </NavCategory>

      <NavCategory title="Serviços">
        <NavItem to="/posts" icon={<PiChatCenteredDots size={20} className={style.navIcon} />} label="Posts" />
      </NavCategory>

      <NavCategory title="Conta">
        <NavItem to="/account" icon={<CiUser size={20} className={style.navIcon} />} label="Minha conta" />
        {user?.role === "ADMIN" && (
          <NavItem
            to="/admin"
            icon={<MdAdminPanelSettings size={20} className={style.navIcon} />}
            label="Administração"
          />
        )}
        <li>
          <button className={style.logoutBtn} onClick={requestLogout}>
            <CiLogout size={20} className={style.navIcon} />
            Logout
          </button>
        </li>
      </NavCategory>
      {logoutModal}
    </nav>
  );
}
