import { useEffect, useRef, useState } from "preact/hooks";

// ascii only: other glyphs fall back to another font and shift the width
const GLYPHS = "01#@$%&*+=<>/\\|{}[]?!";
export const rnd = () => GLYPHS[Math.floor(Math.random() * GLYPHS.length)];

// letters assemble left to right out of random symbols, then glitch now and then;
// re-runs whenever `text` changes
export const Scramble = ({
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

// shimmering symbol field; a slice of cells flips every tick.
// forever=false stops after ~7s (toast is gone by then)
export const AsciiBg = ({
  className,
  cols = 78,
  rows = 12,
  ms = 50,
  forever = false,
}: {
  className: string;
  cols?: number;
  rows?: number;
  ms?: number;
  forever?: boolean;
}) => {
  const ref = useRef<HTMLPreElement>(null);

  useEffect(() => {
    const g = Array.from({ length: cols * rows }, rnd);
    const draw = () => {
      let s = "";
      for (let r = 0; r < rows; r++)
        s += g.slice(r * cols, (r + 1) * cols).join("") + "\n";
      if (ref.current) ref.current.textContent = s;
    };
    draw();
    let f = 0;
    const t = setInterval(() => {
      for (let k = 0; k < cols * rows * 0.12; k++)
        g[Math.floor(Math.random() * g.length)] = rnd();
      draw();
      if (!forever && ++f > 140) clearInterval(t);
    }, ms);
    return () => clearInterval(t);
  }, []);

  return <pre ref={ref} className={className} aria-hidden="true" />;
};
