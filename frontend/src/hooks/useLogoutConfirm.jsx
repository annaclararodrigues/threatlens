import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import ConfirmModal from "../components/ConfirmModal/ConfirmModal";

// Logout com confirmação. Usado na sidebar e no menu do avatar:
// requestLogout abre o modal; renderize logoutModal no componente.
export function useLogoutConfirm() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleConfirm = async () => {
    setLoading(true);
    await logout();
    navigate("/login", { replace: true });
  };

  const logoutModal = (
    <ConfirmModal
      isOpen={open}
      title="Sair da conta"
      message="Tem certeza que deseja sair? Você precisará fazer login novamente para acessar o sistema."
      confirmLabel="Sair"
      onConfirm={handleConfirm}
      onCancel={() => setOpen(false)}
      loading={loading}
    />
  );

  return { requestLogout: () => setOpen(true), logoutModal };
}
