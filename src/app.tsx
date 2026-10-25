import { useEffect, useState } from "preact/hooks";
import Header from "./layout/Header/Header";
import Hud from "./layout/Hud/Hud";
import Eyes from "./layout/Eyes/Eyes";

export function App() {
  const [stage, setStage] = useState(0);

  useEffect(() => {
    const a = setTimeout(() => setStage(1), 4600);
    const b = setTimeout(() => setStage(2), 7200);
    return () => {
      clearTimeout(a);
      clearTimeout(b);
    };
  }, []);

  return (
    <>
      <Eyes />
      {stage >= 1 && <Hud />}
      {stage >= 2 && <Header />}
    </>
  );
}
