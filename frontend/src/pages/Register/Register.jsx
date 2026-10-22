import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authService } from "../../services/auth/authService";
import { EMAIL_VERIFICATION_KEY, CODE_TYPE_KEY, CODE_TYPE_REGISTER } from "../../constants";
import { storage } from "../../utils";
import { useToast } from "../../context/ToastContext";
import PasswordInput from "../../components/PasswordInput/PasswordInput";
import AuthLayout, { AuthError, AuthSubmit } from "../../components/AuthLayout/AuthLayout";
import style from "../../components/AuthLayout/AuthLayout.module.css";

export default function Register() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [repeatPassword, setRepeatPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { success } = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== repeatPassword) {
      setError("As senhas não coincidem.");
      return;
    }

    setLoading(true);
    try {
      await authService.register({ username, email, password });
      storage.set(EMAIL_VERIFICATION_KEY, email);
      storage.set(CODE_TYPE_KEY, CODE_TYPE_REGISTER);
      success("Cadastro realizado! Verifique seu e-mail.");
      navigate("/verify");
    } catch (err) {
      setError(err.response?.data?.message ?? "Erro ao cadastrar.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout
      subtitle="Crie sua conta para acessar o painel"
      footer={
        <>
          Já tem conta?{" "}
          <Link to="/login" className={style.link}>
            Entrar
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className={style.form}>
        <div className={style.field}>
          <label htmlFor="username" className={style.label}>Nome</label>
          <input
            id="username"
            type="text"
            autoComplete="name"
            placeholder="Seu nome"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            autoFocus
          />
        </div>

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
          />
        </div>

        <div className={style.field}>
          <label htmlFor="password" className={style.label}>Senha</label>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>

        <div className={style.field}>
          <label htmlFor="repeatPassword" className={style.label}>Confirmar senha</label>
          <PasswordInput
            id="repeatPassword"
            autoComplete="new-password"
            placeholder="••••••••"
            value={repeatPassword}
            onChange={(e) => setRepeatPassword(e.target.value)}
            required
          />
        </div>

        <AuthError>{error}</AuthError>
        <AuthSubmit loading={loading} loadingLabel="Criando conta...">Criar conta</AuthSubmit>
      </form>
    </AuthLayout>
  );
}
