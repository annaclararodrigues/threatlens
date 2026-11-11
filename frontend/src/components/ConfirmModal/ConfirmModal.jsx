import { createPortal } from "react-dom";
import style from "./ConfirmModal.module.css";

export default function ConfirmModal({
  isOpen,
  title,
  message,
  onConfirm,
  onCancel,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  loading = false,
}) {
  if (!isOpen) return null;

  // Portal no body: dentro da sidebar (fixed + transform no mobile) o modal ficaria preso a ela
  return createPortal(
    <>
      <div className={style.overlay} onClick={onCancel} />
      <div className={style.modal} role="dialog" aria-modal="true">
        <h3 className={style.title}>{title}</h3>
        {message && <p className={style.message}>{message}</p>}
        <div className={style.actions}>
          <button className={style.cancelBtn} onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button className={style.confirmBtn} onClick={onConfirm} disabled={loading}>
            {loading ? "Aguarde..." : confirmLabel}
          </button>
        </div>
      </div>
    </>,
    document.body
  );
}
