import { useEffect, useRef, useState } from "preact/hooks";
import style from "./Header.module.scss";
import { biom } from "./Header.type";

const GLYPHS = "01#@$%&*+=<>/\\|{}[]?!";
const rnd = () => GLYPHS[Math.floor(Math.random() * GLYPHS.length)];

const Scramble = ({
  text,
  start = 0,
  rate = 0.5,
}: {
  text: string;
  start?: number;
  rate?: number;
}) => {
  const [out, setOut] = useState(() =>
    [...text].map((c) => (c === " " ? " " : rnd())).join(""),
  );

  useEffect(() => {
    let f = 0;
    const t = setInterval(() => {
      f++;
      const done = (f - start) * rate;
      setOut(
        [...text]
          .map((c, i) => {
            if (c === " ") return c;
            if (i >= done) return f < start ? " " : rnd();
            return Math.random() < 0.015 ? rnd() : c;
          })
          .join(""),
      );
      if (f > 170) clearInterval(t);
    }, 40);
    return () => clearInterval(t);
  }, [text]);

  return <>{out}</>;
};

const COLS = 78;
const ROWS = 12;

const AsciiBg = () => {
  const ref = useRef<HTMLPreElement>(null);

  useEffect(() => {
    const g = Array.from({ length: COLS * ROWS }, rnd);
    const draw = () => {
      let s = "";
      for (let r = 0; r < ROWS; r++)
        s += g.slice(r * COLS, (r + 1) * COLS).join("") + "\n";
      if (ref.current) ref.current.textContent = s;
    };
    draw();
    let f = 0;
    const t = setInterval(() => {
      for (let k = 0; k < 90; k++)
        g[Math.floor(Math.random() * g.length)] = rnd();
      draw();
      if (++f > 140) clearInterval(t);
    }, 50);
    return () => clearInterval(t);
  }, []);

  return <pre ref={ref} className={style.bg} aria-hidden="true" />;
};

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
        <AsciiBg />
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
