import { useState } from "react";
import style from "./MyAccount.module.css";
import Header from "../../components/Header/Header";
import Avatar from "../../components/Avatar/Avatar";
import RoleBadge from "../../components/RoleBadge/RoleBadge";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { authService } from "../../services/auth/authService";
import PasswordInput from "../../components/PasswordInput/PasswordInput";

// Os dados do perfil são só leitura: o backend ainda não permite alterar nome ou email
export default function MyAccount() {
  const { user } = useAuth();
  const { success } = useToast();

  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [modalError, setModalError] = useState("");
  const [saving, setSaving] = useState(false);

  function openPasswordModal() {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setModalError("");
    setPasswordModalOpen(true);
  }

  function closePasswordModal() {
    setPasswordModalOpen(false);
  }

  async function handleChangePassword(e) {
    e.preventDefault();
    setModalError("");

    if (!currentPassword.trim() || !newPassword.trim() || !confirmPassword.trim()) {
      setModalError("Preencha todos os campos.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setModalError("As senhas não coincidem.");
      return;
    }

    setSaving(true);
    try {
      await authService.changePassword({
        currentPassword,
        newPassword,
        newPasswordConfirm: confirmPassword,
      });
      closePasswordModal();
      success("Senha alterada com sucesso.");
    } catch (err) {
      setModalError(err.response?.data?.message ?? "Erro ao alterar a senha.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <Header
        pageName="Minha conta"
        pageDescription="Gerencie as informações da sua conta"
      />

      <div className={style.cards}>
        <section className={style.card} aria-labelledby="profile-title">
          <h3 id="profile-title" className={style.cardTitle}>Perfil</h3>
          <div className={style.profile}>
            <Avatar name={user?.username} size="lg" />
            <div className={style.profileInfo}>
              <span className={style.name}>{user?.username}</span>
              <span className={style.email}>{user?.email}</span>
              <RoleBadge role={user?.role} />
            </div>
          </div>
        </section>

        <section className={style.card} aria-labelledby="security-title">
          <h3 id="security-title" className={style.cardTitle}>Segurança</h3>
          <div className={style.row}>
            <div>
              <span className={style.rowLabel}>Senha</span>
              <p className={style.rowHint}>Use uma senha forte que você não use em outros sites.</p>
            </div>
            <button type="button" className={style.secondaryBtn} onClick={openPasswordModal}>
              Alterar senha
            </button>
          </div>
        </section>
      </div>

      {passwordModalOpen && (
        <>
          <div className={style.overlay} onClick={closePasswordModal} />
          <form className={style.modal} role="dialog" aria-modal="true" aria-labelledby="password-title" onSubmit={handleChangePassword}>
            <h3 id="password-title" className={style.modalTitle}>Alterar senha</h3>
            <div className={style.modalField}>
              <label htmlFor="currentPassword">Senha atual</label>
              <PasswordInput
                id="currentPassword"
                placeholder="Digite sua senha atual"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
                autoFocus
              />
            </div>
            <div className={style.modalField}>
              <label htmlFor="newPassword">Nova senha</label>
              <PasswordInput
                id="newPassword"
                placeholder="Digite a nova senha"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                autoComplete="new-password"
              />
            </div>
            <div className={style.modalField}>
              <label htmlFor="confirmPassword">Confirmar nova senha</label>
              <PasswordInput
                id="confirmPassword"
                placeholder="Repita a nova senha"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                autoComplete="new-password"
              />
            </div>
            {modalError && <p className={style.modalError}>{modalError}</p>}
            <div className={style.modalActions}>
              <button type="button" className={style.cancelBtn} onClick={closePasswordModal}>
                Cancelar
              </button>
              <button type="submit" className={style.saveBtn} disabled={saving}>
                {saving ? "Salvando..." : "Alterar senha"}
              </button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}
