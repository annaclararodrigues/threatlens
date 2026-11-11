import { useEffect, useMemo, useRef, useState } from "react";
import * as d3 from "d3";
import cloud from "d3-cloud";
import { useNavigate } from "react-router-dom";
import style from "./WordCloud.module.css";
import { useTheme } from "../../hooks";
import { cssVar } from "../../utils/theme";

const MAX_WORDS = 30;
const MIN_FONT = 13;
const MAX_FONT = 34;

// Fragmentos de URL e ruído que não dizem nada ao analista
const NOISE = new Set(["http", "https", "www", "com", "org", "net", "html", "htm", "php", "amp", "utm", "t.me", "ref"]);

function isNoise(text) {
  const word = text.toLowerCase();
  return word.length < 3 || NOISE.has(word) || word.startsWith("utm") || /^\d+$/.test(word);
}

const WordCloud = ({ palavras, loading = false, error, linkForWord }) => {
  const navigate = useNavigate();
  const isLight = useTheme();
  const svgRef = useRef(null);
  // Ref para o desenho não depender da identidade da função (evita redesenhar a cada render)
  const onWordClickRef = useRef(null);
  onWordClickRef.current = linkForWord ? (word) => navigate(linkForWord(word)) : null;
  const containerRef = useRef(null);
  const [dimensions, setDimensions] = useState(null);

  useEffect(() => {
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          setDimensions({ width, height });
        }
      }
    });

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const words = useMemo(() => palavras.filter((p) => !isNoise(p.text)).slice(0, MAX_WORDS), [palavras]);
  // A consulta da nuvem é a mais lenta do dashboard: durante a carga não mostra a nuvem antiga
  // (seria de outro período) nem a mensagem de vazio (pareceria que não há dados)
  const message = loading
    ? null
    : error
      ? "Não foi possível carregar os termos."
      : words.length === 0 ? "Sem termos suficientes no período." : null;

  useEffect(() => {
    if (!dimensions || loading || message) return;

    const { width, height } = dimensions;
    const maxValue = d3.max(words, (d) => d.value) || 1;
    const minValue = d3.min(words, (d) => d.value) || 0;
    const fontScale = d3.scaleSqrt()
      .domain([minValue, Math.max(maxValue, minValue + 1)])
      .range([MIN_FONT, MAX_FONT]);
    // Quanto maior a palavra, mais contraste: evita uma nuvem chapada num tom só
    const colorScale = d3.scaleLinear()
      .domain([MIN_FONT, MAX_FONT])
      .range([cssVar("--color-word-weak"), cssVar("--color-word-strong")])
      .interpolate(d3.interpolateRgb);

    const layout = cloud()
      .size([width, height])
      .words(words.map((p) => ({ text: p.text, size: fontScale(p.value) })))
      .padding(4)
      .rotate(() => 0)
      .font("Arial")
      .fontSize((d) => d.size)
      .on("end", draw);

    layout.start();

    function draw(placed) {
      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();

      svg
        .attr("viewBox", `0 0 ${width} ${height}`)
        .append("g")
        .attr("transform", `translate(${width / 2}, ${height / 2})`)
        .selectAll("text")
        .data(placed)
        .enter()
        .append("text")
        .attr("class", style.word)
        .attr("text-anchor", "middle")
        .attr("transform", (d) => `translate(${d.x}, ${d.y}) rotate(${d.rotate})`)
        // Atributo (e não style) para o :hover do CSS continuar valendo
        .attr("fill", (d) => colorScale(d.size))
        .style("font-size", (d) => `${d.size}px`)
        .text((d) => d.text)
        .on("click", (_event, d) => onWordClickRef.current?.(d.text))
        .append("title")
        .text((d) => `Ver posts com "${d.text}"`);
    }

    return () => layout.stop();
  }, [words, dimensions, loading, message, isLight]);

  return (
    <div className={style.wordCloudContainer}>
      <h2 className={style.description}>Nuvem de palavras</h2>
      <div className={style.wordCloudComponent} ref={containerRef} aria-busy={loading}>
        {loading ? (
          <div className={style.loading} role="status">
            <span className={style.spinner} aria-hidden="true" />
            Carregando termos…
          </div>
        ) : message ? (
          <p className={style.empty}>{message}</p>
        ) : (
          <svg ref={svgRef} className={style.svg} />
        )}
      </div>
    </div>
  );
};

export default WordCloud;
