import style from './Header.module.css';
import { IoMoon, IoSunny } from "react-icons/io5";
import { PiSignOut, PiUser } from "react-icons/pi";
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLogoutConfirm } from '../../hooks';
import Avatar from '../Avatar/Avatar';
import DropdownMenu from '../DropdownMenu/DropdownMenu';
import { applyTheme, isStoredThemeLight } from '../../utils/theme';

export default function Header(props) {
    const navigate = useNavigate();
    const { user } = useAuth();
    const { requestLogout, logoutModal } = useLogoutConfirm();
    const [isLight, setIsLight] = useState(isStoredThemeLight);

    useEffect(() => {
        applyTheme(isLight);
    }, [isLight]);

    return (
        <header className={style.header}>
            <div className={style.welcome}>
                <h2>{props.pageName}</h2>
                <p>{props.pageDescription}</p>
            </div>
            <div className={style.headerIcons}>
                <DropdownMenu
                    label="Menu da conta"
                    triggerClassName={style.avatarBtn}
                    trigger={<Avatar name={user?.username} />}
                    header={
                        <>
                            <p className={style.menuName}>{user?.username}</p>
                            <p className={style.menuEmail}>{user?.email}</p>
                        </>
                    }
                    items={[
                        { label: "Minha conta", icon: <PiUser />, onClick: () => navigate('/account') },
                        { label: "Sair", icon: <PiSignOut />, onClick: requestLogout, danger: true, separated: true },
                    ]}
                />
                <label className={style.switch} title={isLight ? "Mudar para modo escuro" : "Mudar para modo claro"}>
                    <input
                        type="checkbox"
                        checked={isLight}
                        onChange={() => setIsLight(!isLight)}
                        aria-label="Alternar entre modo claro e escuro"
                    />
                    <span className={style.slider}>
                        <IoMoon className={`${style.trackIcon} ${style.moonIcon}`} aria-hidden="true" />
                        <IoSunny className={`${style.trackIcon} ${style.sunIcon}`} aria-hidden="true" />
                        <span className={style.knob}>
                            {isLight ? <IoSunny aria-hidden="true" /> : <IoMoon aria-hidden="true" />}
                        </span>
                    </span>
                </label>
            </div>
            {logoutModal}
        </header>
    )
}