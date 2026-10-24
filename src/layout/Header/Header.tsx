import { useEffect, useState } from "preact/hooks";
import style from "./Header.module.scss";
import { biom } from "./Header.type";
import { AsciiBg, Scramble } from "../Ascii";

const Header = () => {
  const [state, setState] = useState({ i: 0, n: 0 });
  const b = biom[state.i];

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() === "b")
        setState((s) => ({ i: (s.i + 1) % biom.length, n: s.n + 1 }));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <section className={style.header}>
      <div
        key={state.n}
        className={style.toast}
        style={{ ["--accent" as string]: b.color }}
      >
        <AsciiBg className={style.bg} />
        <div className={`container ${style.container}`}>
          <h2>
            <Scramble text={b.name} rate={0.4} />
          </h2>
          <p>
            <Scramble text={b.info} start={14} rate={2.5} />
          </p>
        </div>
      </div>
    </section>
  );
};

export default Header;
