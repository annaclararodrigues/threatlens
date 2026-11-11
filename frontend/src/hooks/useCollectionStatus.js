import { useEffect, useState } from "react";
import { getCollectionStatus } from "../services/post/postService";

// A cada minuto: atualiza a "última coleta" e, de quebra, o texto relativo ("há 4 minutos")
const REFRESH_MS = 60 * 1000;

export function useCollectionStatus() {
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const load = () => {
      getCollectionStatus()
        .then((data) => {
          if (!cancelled) {
            setStatus(data);
            setError(null);
          }
        })
        .catch((err) => { if (!cancelled) setError(err); });
    };

    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return { status, error };
}
