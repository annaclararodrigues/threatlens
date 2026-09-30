import { useState } from "react";
import { MdDeleteOutline } from "react-icons/md";
import style from "./Admin.module.css";
import Header from "../../components/Header/Header";
import ConfirmModal from "../../components/ConfirmModal/ConfirmModal";
import { useUsers } from "../../hooks";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";

export default function Admin() {
  const [page, setPage] = useState(0);
  const { users, pagination, loading, error, deleteUser } = useUsers({ page, size: 20 });
  const { user: currentUser } = useAuth();
  const { success, error: notifyError } = useToast();
  const [targetUser, setTargetUser] = useState(null);
  const [deleting, setDeleting] = useState(false);

  async function handleConfirmDelete() {
    setDeleting(true);
    try {
      await deleteUser(targetUser.id);
      setTargetUser(null);
      success("Usuário removido com sucesso.");
      if (users.length === 1 && page > 0) setPage((p) => p - 1);
    } catch {
      notifyError("Erro ao deletar usuário.");
      setTargetUser(null);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className={style.admin}>
      <Header
        pageName="Administração"
        pageDescription="Gerencie os usuários da plataforma"
      />

      <div className={style.content}>
        {loading && <p className={style.message}>Carregando usuários...</p>}
        {error && <p className={style.error}>{error}</p>}
        {!loading && !error && users.length === 0 && (
          <p className={style.message}>Nenhum usuário encontrado.</p>
        )}

        <div className={style.grid}>
          {users.map((u) => (
            <div key={u.id} className={style.card}>
              <div className={style.cardInfo}>
                <span className={style.name}>{u.username}</span>
                <span className={style.email}>{u.email}</span>
              </div>
              <button
                className={style.deleteBtn}
                onClick={() => setTargetUser(u)}
                title={u.email === currentUser?.email ? "Você não pode deletar sua própria conta" : "Deletar conta"}
                disabled={u.email === currentUser?.email}
              >
                <MdDeleteOutline size={20} />
              </button>
            </div>
          ))}
        </div>

        {pagination && pagination.totalPages > 1 && (
          <div className={style.pagination}>
            <button
              className={style.pageBtn}
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Anterior
            </button>
            <span className={style.pageInfo}>
              Página {pagination.page + 1} de {pagination.totalPages}
            </span>
            <button
              className={style.pageBtn}
              disabled={page >= pagination.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
            >
              Próxima
            </button>
          </div>
        )}
      </div>

      <ConfirmModal
        isOpen={targetUser !== null}
        title="Deletar conta"
        message={`Tem certeza que deseja deletar a conta de ${targetUser?.username}? Essa ação não pode ser desfeita.`}
        confirmLabel="Deletar"
        onConfirm={handleConfirmDelete}
        onCancel={() => setTargetUser(null)}
        loading={deleting}
      />
    </div>
  );
}
