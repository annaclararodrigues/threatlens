import { useCallback, useEffect, useState } from "react";
import { adminService } from "../services/admin/adminService";

// role (opcional): lista só os usuários com esse papel
export function useUsers({ page = 0, size = 20, role } = {}) {
  const [users, setUsers] = useState([]);
  const [pagination, setPagination] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const { data } = await adminService.listAll({ page, size, role });
      setUsers(data.content);
      setPagination({ page: data.page, size: data.size, totalElements: data.totalElements, totalPages: data.totalPages });
    } catch {
      setError("Erro ao carregar usuários.");
    } finally {
      setLoading(false);
    }
  }, [page, size, role]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  function removeFromList(id) {
    setUsers((prev) => prev.filter((u) => u.id !== id));
    setPagination((prev) => prev && { ...prev, totalElements: prev.totalElements - 1 });
  }

  async function deleteUser(id) {
    await adminService.deleteUser(id);
    removeFromList(id);
  }

  async function updateRole(id, newRole) {
    await adminService.updateRole(id, newRole);
    // Com o filtro por papel ativo, quem mudou de papel deixa de pertencer à lista
    if (role && role !== newRole) removeFromList(id);
    else setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, role: newRole } : u)));
  }

  return { users, pagination, loading, error, deleteUser, updateRole };
}
