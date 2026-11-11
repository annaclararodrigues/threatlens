import { useState } from "react";
import { PiDotsThreeBold, PiShieldCheck, PiTrash, PiUser } from "react-icons/pi";
import style from "./Admin.module.css";
import Header from "../../components/Header/Header";
import ConfirmModal from "../../components/ConfirmModal/ConfirmModal";
import Avatar from "../../components/Avatar/Avatar";
import RoleBadge from "../../components/RoleBadge/RoleBadge";
import DropdownMenu from "../../components/DropdownMenu/DropdownMenu";
import { useUsers } from "../../hooks";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { roleLabel } from "../../utils/user";

// "" = todos; os demais são valores do enum Role do backend
const ROLE_FILTERS = [
  { value: "", label: "Todos" },
  { value: "ADMIN", label: "Admins" },
  { value: "USER", label: "Analistas" },
];

export default function Admin() {
  const [page, setPage] = useState(0);
  const [roleFilter, setRoleFilter] = useState("");
  const { users, pagination, loading, error, deleteUser, updateRole } = useUsers({
    page,
    size: 20,
    role: roleFilter || undefined,
  });
  const { user: currentUser } = useAuth();
  const { success, error: notifyError } = useToast();
  const [targetUser, setTargetUser] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const total = pagination?.totalElements ?? users.length;

  function handleRoleFilterChange(value) {
    setRoleFilter(value);
    setPage(0);
  }

  async function handleConfirmDelete() {
    setDeleting(true);
    try {
      await deleteUser(targetUser.id);
      setTargetUser(null);
      success("Usuário excluído com sucesso.");
      if (users.length === 1 && page > 0) setPage((p) => p - 1);
    } catch {
      notifyError("Erro ao excluir usuário.");
      setTargetUser(null);
    } finally {
      setDeleting(false);
    }
  }

  async function handleRoleChange(target, role) {
    try {
      await updateRole(target.id, role);
      success(`${target.username} agora é ${roleLabel(role)}.`);
    } catch (err) {
      // Ex.: o backend não permite remover o último administrador
      notifyError(err.response?.data?.message ?? "Erro ao alterar o papel.");
    }
  }

  function actionsFor(target) {
    const nextRole = target.role === "ADMIN" ? "USER" : "ADMIN";
    return [
      {
        label: nextRole === "ADMIN" ? "Tornar admin" : "Tornar analista",
        icon: nextRole === "ADMIN" ? <PiShieldCheck /> : <PiUser />,
        onClick: () => handleRoleChange(target, nextRole),
      },
      {
        label: "Excluir usuário",
        icon: <PiTrash />,
        onClick: () => setTargetUser(target),
        danger: true,
        separated: true,
      },
    ];
  }

  return (
    <div>
      <Header
        pageName="Administração"
        pageDescription="Gerencie os usuários da plataforma"
      />

      <div className={style.content}>
        <div className={style.toolbar}>
          <h3 className={style.count}>
            {total} {total === 1 ? "usuário" : "usuários"}
          </h3>
          <div className={style.segmented} role="group" aria-label="Filtrar por papel">
            {ROLE_FILTERS.map((filter) => {
              const selected = roleFilter === filter.value;
              return (
                <button
                  key={filter.value}
                  type="button"
                  className={`${style.segment} ${selected ? style.segmentSelected : ""}`}
                  aria-pressed={selected}
                  onClick={() => handleRoleFilterChange(filter.value)}
                >
                  {filter.label}
                </button>
              );
            })}
          </div>
        </div>

        {loading && <p className={style.message}>Carregando usuários...</p>}
        {error && <p className={style.error}>{error}</p>}
        {!loading && !error && users.length === 0 && (
          <p className={style.message}>
            {roleFilter ? "Nenhum usuário com esse papel." : "Nenhum usuário encontrado."}
          </p>
        )}

        {users.length > 0 && (
          <div className={style.tableCard}>
            <table className={style.table}>
              <thead>
                <tr>
                  <th scope="col">Usuário</th>
                  <th scope="col">Papel</th>
                  <th scope="col">Status</th>
                  <th scope="col"><span className={style.srOnly}>Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const isSelf = u.email === currentUser?.email;
                  return (
                    <tr key={u.id}>
                      <td>
                        <div className={style.userCell}>
                          <Avatar name={u.username} size="sm" />
                          <div className={style.userInfo}>
                            <span className={style.name}>
                              {u.username}
                              {isSelf && <span className={style.selfBadge}>Você</span>}
                            </span>
                            <span className={style.email}>{u.email}</span>
                          </div>
                        </div>
                      </td>
                      <td><RoleBadge role={u.role} /></td>
                      <td>
                        <span className={`${style.status} ${u.isEmailVerified ? style.verified : style.pending}`}>
                          {u.isEmailVerified ? "Ativo" : "Aguardando verificação"}
                        </span>
                      </td>
                      <td className={style.actionsCell}>
                        {/* Sem ações na própria linha: não dá para excluir nem rebaixar a si mesmo */}
                        {!isSelf && (
                          <DropdownMenu
                            label={`Ações para ${u.username}`}
                            triggerClassName={style.menuBtn}
                            trigger={<PiDotsThreeBold aria-hidden="true" />}
                            items={actionsFor(u)}
                          />
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

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
        title="Excluir usuário"
        message={
          <>
            A conta de <strong>{targetUser?.username}</strong> ({targetUser?.email}) será excluída
            permanentemente. Essa ação não pode ser desfeita.
          </>
        }
        confirmLabel="Excluir usuário"
        onConfirm={handleConfirmDelete}
        onCancel={() => setTargetUser(null)}
        loading={deleting}
      />
    </div>
  );
}
