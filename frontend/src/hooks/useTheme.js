import { useState, useEffect } from "react";

export function useTheme() {
  const [isLight, setIsLight] = useState(() => document.body.classList.contains("light"));

  useEffect(() => {
    const sync = () => setIsLight(document.body.classList.contains("light"));
    const observer = new MutationObserver(sync);

    observer.observe(document.body, { attributes: true, attributeFilter: ["class"] });
    // O tema pode ter sido aplicado (pelo Header) antes deste observer existir
    sync();

    return () => observer.disconnect();
  }, []);

  return isLight;
}
