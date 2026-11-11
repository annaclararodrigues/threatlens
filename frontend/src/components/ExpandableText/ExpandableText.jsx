import { useState } from "react";
import style from "./ExpandableText.module.css";

// Texto longo começa recolhido em `lines` linhas, com um botão para ver tudo
export default function ExpandableText({ text, as: Tag = "div", className, lines = 8, chars = 600, empty = "Sem conteúdo" }) {
  const [expanded, setExpanded] = useState(false);
  const long = text.split("\n").length > lines || text.length > chars;

  return (
    <>
      <Tag className={className}>
        {/* O corte fica num elemento interno para não vazar no padding da caixa */}
        <div className={long && !expanded ? style.collapsed : undefined} style={{ "--lines": lines }}>
          {text || empty}
        </div>
      </Tag>
      {long && (
        <button type="button" className={style.toggle} onClick={() => setExpanded((e) => !e)}>
          {expanded ? "Mostrar menos" : "Mostrar tudo"}
        </button>
      )}
    </>
  );
}
