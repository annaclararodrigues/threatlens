import { useEffect, useState } from "react";
import style from "./Posts.module.css";
import Header from "../../components/Header/Header";
import AllPostsTable from "../../components/AllPostsTable/AllPostsTable";
import { usePosts } from "../../hooks";

export default function Posts() {
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("DATE");
  const [order, setOrder] = useState("DESC");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const { posts, pagination, loading } = usePosts({ page, size: 20, sort, order, search });

  useEffect(() => {
    const timeout = setTimeout(() => {
      setPage(0);
      setSearch(searchInput.trim());
    }, 400);
    return () => clearTimeout(timeout);
  }, [searchInput]);

  const handleSort = (field) => {
    setPage(0);
    if (sort === field) {
      setOrder((o) => (o === "ASC" ? "DESC" : "ASC"));
    } else {
      setSort(field);
      setOrder("ASC");
    }
  };

  return (
    <div className={style.posts}>
      <Header pageName="Posts" pageDescription="Analise os posts inseridos no sistema." />

      <div className={style.postsInputs}>
        <input
          type="text"
          placeholder="Buscar posts..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
        />
      </div>

      <div className={style.postsContainer}>
        <AllPostsTable
          posts={posts}
          loading={loading}
          sort={sort}
          order={order}
          onSort={handleSort}
        />

        {pagination && (
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
    </div>
  );
}
