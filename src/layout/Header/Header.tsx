import { useEffect, useState } from "preact/hooks";
import style from "./Header.module.scss";
import { places } from "./Header.type";
import { AsciiBg, Scramble } from "../Ascii";

const Header = () => {
  const [state, setState] = useState({ i: 0, n: 0 });
  const p = places[state.i];

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() === "b")
        setState((s) => ({ i: (s.i + 1) % places.length, n: s.n + 1 }));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <section className={style.header}>
      <div
        key={state.n}
        className={`${style.toast} ${p.realm === "overworld" ? "" : style[p.realm]}`}
        style={{ ["--accent" as string]: p.color }}
      >
        {p.flash && (
          <div
            className={style.flash}
            style={{ ["--fc" as string]: p.flash }}
          />
        )}
        <AsciiBg
          className={style.bg}
          glyphs={p.glyphs}
          flow={p.flow}
          density={p.density}
          ms={p.speed}
        />
        <div className={`container ${style.container}`}>
          <span className={style.label}>
            <Scramble text={p.label} glyphs={p.glyphs} rate={0.9} />
          </span>
          <h2 className={style[p.motion]}>
            <Scramble text={p.name} glyphs={p.glyphs} rate={0.4} />
          </h2>
          <p>
            <Scramble text={p.info} glyphs={p.glyphs} start={14} rate={2.5} />
          </p>
        </div>
      </div>
    </section>
  );
};

export default Header;
