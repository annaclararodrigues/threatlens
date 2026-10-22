import style from "./RelevantPostsTable.module.css";
import { useState } from "react";
import { Link } from "react-router-dom";
import { FiChevronRight } from "react-icons/fi";
import PostDrawer from "../PostDrawer/PostDrawer";
import ScorePill from "../ScorePill/ScorePill";
import { cleanText, formatRelativeTime, postOrigin } from "../../utils/format";

export default function RelevantPostsTable({ mostRelevantPosts, error, seeAllLink = "/posts" }) {
  const [selectedIndex, setSelectedIndex] = useState(null);
  const selectedPost = selectedIndex != null ? mostRelevantPosts[selectedIndex] ?? null : null;

  return (
    <div className={style.listContainer}>
      <div className={style.header}>
        <h2 className={style.description}>Posts mais relevantes</h2>
        <Link to={seeAllLink} className={style.seeAll}>Ver todos →</Link>
      </div>

      {error ? (
        <p className={style.empty}>Não foi possível carregar os posts.</p>
      ) : mostRelevantPosts.length === 0 ? (
        <p className={style.empty}>Nenhum post no período.</p>
      ) : (
        <ul className={style.list}>
          {mostRelevantPosts.map((post, index) => {
            const score = post.classification?.score ?? post.rate;
            const text = cleanText(post.title ?? post.content);
            const origin = postOrigin(post);
            const time = formatRelativeTime(post.createdAt ?? post.created_at);
            const hasIoc = post.classification?.ioc;

            return (
              <li key={post.id}>
                <button
                  type="button"
                  className={`${style.item} ${index === selectedIndex ? style.itemSelected : ""}`}
                  onClick={() => setSelectedIndex(index)}
                >
                  <ScorePill score={score} />
                  <span className={style.body}>
                    <span className={style.title} title={text}>{text || "Sem conteúdo"}</span>
                    <span className={style.meta}>
                      {[origin, time].filter(Boolean).join(" · ")}
                      {hasIoc && <span className={style.tag}>IoC</span>}
                    </span>
                  </span>
                  <FiChevronRight className={style.chevron} aria-hidden="true" />
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <PostDrawer
        post={selectedPost}
        onClose={() => setSelectedIndex(null)}
        onPrev={selectedIndex > 0 ? () => setSelectedIndex(selectedIndex - 1) : null}
        onNext={selectedIndex != null && selectedIndex < mostRelevantPosts.length - 1 ? () => setSelectedIndex(selectedIndex + 1) : null}
      />
    </div>
  );
}
