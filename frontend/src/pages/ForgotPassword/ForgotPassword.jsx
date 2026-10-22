import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authService } from "../../services/auth/authService";
import { EMAIL_VERIFICATION_KEY, CODE_TYPE_KEY, CODE_TYPE_RESET_PASSWORD } from "../../constants";
import { storage } from "../../utils";
import { useToast } from "../../context/ToastContext";
import AuthLayout, { AuthError, AuthSubmit } from "../../components/AuthLayout/AuthLayout";
import style from "../../components/AuthLayout/AuthLayout.module.css";

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
    <AuthLayout
      subtitle="Informe seu e-mail para receber um código de recuperação"
      footer={
        <>
          Lembrou a senha?{" "}
          <Link to="/login" className={style.link}>
            Voltar ao login
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className={style.form}>
        <div className={style.field}>
          <label htmlFor="email" className={style.label}>E-mail</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="voce@empresa.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </div>

        <AuthError>{error}</AuthError>
        <AuthSubmit loading={loading} loadingLabel="Enviando...">Enviar código</AuthSubmit>
      </form>
    </AuthLayout>
  );
}
