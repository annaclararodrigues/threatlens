import { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar/Sidebar";
import { Outlet, useLocation, useNavigationType } from "react-router-dom";
import style from "./RootLayout.module.css";

// Ao trocar de página, começa do topo. Só olha o caminho (filtros e post aberto mudam
// a query string e não devem mexer na rolagem) e respeita Voltar/Avançar (POP).
function useScrollToTopOnPageChange() {
  const { pathname } = useLocation();
  const navigationType = useNavigationType();

  useEffect(() => {
    if (navigationType !== "POP") window.scrollTo(0, 0);
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps
}

export default function RootLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  useScrollToTopOnPageChange();

  return (
    <>
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      {sidebarOpen && (
        <div
          className={style.overlay}
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <button
        className={style.hamburger}
        onClick={() => setSidebarOpen(!sidebarOpen)}
        aria-label={sidebarOpen ? "Fechar menu" : "Abrir menu"}
        aria-expanded={sidebarOpen}
      >
        <span></span>
        <span></span>
        <span></span>
      </button>
      <main className={style.content}>
        <Outlet />
      </main>
    </>
  );
}
