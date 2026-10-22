import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authService } from "../../services/auth/authService";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import PasswordInput from "../../components/PasswordInput/PasswordInput";
import AuthLayout, { AuthError, AuthSubmit } from "../../components/AuthLayout/AuthLayout";
import style from "../../components/AuthLayout/AuthLayout.module.css";

export default function ResetPassword() {
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { success } = useToast();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== passwordConfirm) {
      setError("As senhas não coincidem.");
      return;
    }

    setLoading(true);
    try {
      await authService.resetPassword({ password, passwordConfirm });
      await logout();
      success("Senha redefinida. Faça login com sua nova senha.");
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err.response?.data?.message ?? "Não foi possível redefinir a senha.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout subtitle="Defina sua nova senha">
      <form onSubmit={handleSubmit} className={style.form}>
        <div className={style.field}>
          <label htmlFor="password" className={style.label}>Nova senha</label>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoFocus
          />
        </div>

        <div className={style.field}>
          <label htmlFor="passwordConfirm" className={style.label}>Confirmar nova senha</label>
          <PasswordInput
            id="passwordConfirm"
            autoComplete="new-password"
            placeholder="••••••••"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            required
          />
        </div>

        <AuthError>{error}</AuthError>
        <AuthSubmit loading={loading} loadingLabel="Salvando...">Redefinir senha</AuthSubmit>
      </form>
    </AuthLayout>
  );
}
