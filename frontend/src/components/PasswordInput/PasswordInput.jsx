import { useState } from "react";
import { PiEye, PiEyeSlash } from "react-icons/pi";
import style from "./PasswordInput.module.css";

// Campo de senha com botão para mostrar/ocultar o que foi digitado.
// Repassa as demais props ao <input>, então herda os estilos de input do formulário.
export default function PasswordInput(props) {
  const [visible, setVisible] = useState(false);

  return (
    <div className={style.wrapper}>
      <input {...props} type={visible ? "text" : "password"} className={style.input} />
      <button
        type="button"
        className={style.toggle}
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
        aria-pressed={visible}
        title={visible ? "Ocultar senha" : "Mostrar senha"}
      >
        {visible ? <PiEyeSlash aria-hidden="true" /> : <PiEye aria-hidden="true" />}
      </button>
    </div>
  );
}
