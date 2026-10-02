import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import style from "./ForgotPassword.module.css";
import { authService } from "../../services/auth/authService";
import { EMAIL_VERIFICATION_KEY, CODE_TYPE_KEY, CODE_TYPE_RESET_PASSWORD } from "../../constants";
import { storage } from "../../utils";
import { useToast } from "../../context/ToastContext";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { success } = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await authService.forgotPassword({ email });
      storage.set(EMAIL_VERIFICATION_KEY, email);
      storage.set(CODE_TYPE_KEY, CODE_TYPE_RESET_PASSWORD);
      success("Código enviado para o seu e-mail.");
      navigate("/verify");
    } catch (err) {
      setError(err.response?.data?.message ?? "Não foi possível enviar o código.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={style.container}>
      <h1>
        Threat<span className={style.highlight}>Lens</span>
      </h1>
      <p>Informe seu e-mail para receber o código de recuperação de senha.</p>
      <form onSubmit={handleSubmit} className={style.form}>
        <input
          type="email"
          placeholder="E-mail"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        {error && <p className={style.errorMsg}>{error}</p>}
        <input type="submit" value={loading ? "Enviando..." : "Enviar código"} disabled={loading} />
      </form>
      <Link to="/login" className={style.backLink}>
        Voltar ao login
      </Link>
    </div>
  );
}
