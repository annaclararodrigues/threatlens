import { useEffect } from "react";

// Fecha popovers e menus: clique fora do elemento (ref) ou tecla Esc chamam onDismiss.
// Só escuta enquanto open for true.
export function useDismiss(ref, open, onDismiss) {
  useEffect(() => {
    if (!open) return;

    const handleClick = (e) => {
      if (!ref.current?.contains(e.target)) onDismiss();
    };
    const handleKey = (e) => {
      if (e.key === "Escape") onDismiss();
    };

    document.addEventListener("mousedown", handleClick);
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("mousedown", handleClick);
      document.removeEventListener("keydown", handleKey);
    };
  }, [ref, open, onDismiss]);
}
