import { createContext, useCallback, useContext, useRef, useState } from "react";
import { createPortal } from "react-dom";
import style from "./ToastContext.module.css";

const ToastContext = createContext(null);
const DEFAULT_DURATION = 4000;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback((type, message, duration = DEFAULT_DURATION) => {
    const id = ++idRef.current;
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => removeToast(id), duration);
  }, [removeToast]);

  const success = useCallback((message, duration) => showToast("success", message, duration), [showToast]);
  const error = useCallback((message, duration) => showToast("error", message, duration), [showToast]);

  return (
    <ToastContext.Provider value={{ success, error }}>
      {children}
      {createPortal(
        <div className={style.toastContainer}>
          {toasts.map((t) => (
            <div
              key={t.id}
              className={`${style.toast} ${style[t.type]}`}
              role="status"
              onClick={() => removeToast(t.id)}
            >
              <span>{t.message}</span>
              <button
                type="button"
                className={style.closeBtn}
                onClick={(e) => { e.stopPropagation(); removeToast(t.id); }}
                aria-label="Fechar"
              >
                ✖
              </button>
            </div>
          ))}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast deve ser usado dentro de ToastProvider");
  return ctx;
}
