import { useState } from "react";
import style from "./AllPostsTable.module.css";
import PostPopUp from "../PostPopUp/PostPopUp";

const SORTABLE_COLUMNS = [
  { field: "ID", label: "ID" },
  { field: "SOURCE", label: "Fonte" },
  { field: "SCORE", label: "Classificação" },
  { field: "CONTENT", label: "Conteúdo" },
];

export default function AllPostsTable({ posts = [], loading = false, sort, order, onSort }) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);

  const handleOpen = (post) => { setSelectedPost(post); setIsOpen(true); };
  const handleClose = () => { setIsOpen(false); setSelectedPost(null); };

  if (loading) return <p style={{ color: "var(--color-text-muted)", padding: "1rem" }}>Carregando...</p>;
  if (!posts.length) return <p style={{ color: "var(--color-text-muted)", padding: "1rem" }}>Nenhum post encontrado.</p>;

  return (
    <>
      <table className={style.postsTable}>
        <thead>
          <tr>
            {SORTABLE_COLUMNS.map(({ field, label }) => (
              <th
                key={field}
                className={onSort ? style.sortable : undefined}
                onClick={() => onSort?.(field)}
              >
                {label}
                <span className={style.sortIndicator}>
                  <span className={sort === field && order === "ASC" ? style.sortActive : undefined}>▲</span>
                  <span className={sort === field && order === "DESC" ? style.sortActive : undefined}>▼</span>
                </span>
              </th>
            ))}
            <th>IoC</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {posts.map((post) => (
            <tr key={post.id}>
              <td>{post.id}</td>
              <td>{post.source}</td>
              <td>{post.classification?.score?.toFixed(2)}</td>
              <td>{(post.title ?? post.content)?.slice(0, 60)}...</td>
              <td>{post.classification?.ioc ? "Sim" : "Não"}</td>
              <td>
                <a href="#" onClick={(e) => { e.preventDefault(); handleOpen(post); }}>
                  Analisar
                </a>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <PostPopUp
        isOpen={isOpen}
        onClose={handleClose}
        id={selectedPost?.id}
        category={selectedPost?.category ?? "-"}
        created_at={selectedPost?.createdAt}
        ioc={selectedPost?.classification?.ioc ? "Sim" : "Não"}
        keyword="-"
        relevant={selectedPost?.classification?.relevant ? "Sim" : "Não"}
        fulltext={selectedPost?.content}
      />
    </>
  );
}
