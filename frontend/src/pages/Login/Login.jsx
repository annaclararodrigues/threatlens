import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authService } from "../../services/auth/authService";
import { useAuth } from "../../context/AuthContext";
import { EMAIL_VERIFICATION_KEY, CODE_TYPE_KEY, CODE_TYPE_REGISTER } from "../../constants";
import { storage } from "../../utils";
import PasswordInput from "../../components/PasswordInput/PasswordInput";
import AuthLayout, { AuthError, AuthSubmit } from "../../components/AuthLayout/AuthLayout";
import style from "../../components/AuthLayout/AuthLayout.module.css";

// Por segurança, a mensagem não diz se o erro foi no e-mail ou na senha
const INVALID_CREDENTIALS = "E-mail ou senha incorretos.";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const { data } = await authService.login({ email, password });
      login(data);
      navigate("/", { replace: true });
    } catch (err) {
      const status = err.response?.status;
      const msg = err.response?.data?.message ?? "";
      // EmailNotVerifiedException: 403 com "E-mail não verificado..."
      if (status === 403 && /verificad/i.test(msg)) {
        storage.set(EMAIL_VERIFICATION_KEY, email);
        storage.set(CODE_TYPE_KEY, CODE_TYPE_REGISTER);
        navigate("/verify");
      } else if (status === 400 || status === 401 || status === 403 || status === 404) {
        setError(INVALID_CREDENTIALS);
      } else if (!err.response) {
        setError("Não foi possível conectar ao servidor. Tente novamente.");
      } else {
        // Ex.: 429 (muitas tentativas) traz uma mensagem útil do backend
        setError(msg || "Erro ao fazer login. Tente novamente.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      subtitle="Faça login para acessar o painel"
      footer={
        <>
          Não tem conta?{" "}
          <Link to="/register" className={style.link}>
            Criar conta
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
            autoComplete="username"
            placeholder="voce@empresa.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </div>

        <div className={style.field}>
          <label htmlFor="password" className={style.label}>Senha</label>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <Link to="/forgot-password" className={style.fieldLink}>
            Esqueci a senha
          </Link>
        </div>

        <AuthError>{error}</AuthError>
        <AuthSubmit loading={loading} loadingLabel="Entrando...">Entrar</AuthSubmit>
      </form>
    </AuthLayout>
  );
}
