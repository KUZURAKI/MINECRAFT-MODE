import { useEffect, useRef, useState } from "preact/hooks";

export const GLYPHS = "01#@$%&*+=<>/\\|{}[]?!";
const pick = (glyphs: string) =>
  glyphs[Math.floor(Math.random() * glyphs.length)];
export const rnd = () => pick(GLYPHS);

export type Flow = "none" | "up" | "down" | "left" | "right";

export const Scramble = ({
  text,
  start = 0,
  rate = 0.5,
  glyphs = GLYPHS,
}: {
  text: string;
  start?: number;
  rate?: number;
  glyphs?: string;
}) => {
  const [out, setOut] = useState(() =>
    [...text].map((c) => (c === " " ? " " : pick(glyphs))).join(""),
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
            if (i >= done) return f < start ? " " : pick(glyphs);
            return Math.random() < 0.015 ? pick(glyphs) : c;
          })
          .join(""),
      );
      if (f > 170) clearInterval(t);
    }, 40);
    return () => clearInterval(t);
  }, [text]);

  return <>{out}</>;
};

export const AsciiBg = ({
  className,
  cols = 78,
  rows = 12,
  ms = 50,
  forever = false,
  glyphs = GLYPHS,
  flow = "none",
  density = 1,
}: {
  className: string;
  cols?: number;
  rows?: number;
  ms?: number;
  forever?: boolean;
  glyphs?: string;
  flow?: Flow;
  density?: number;
}) => {
  const ref = useRef<HTMLPreElement>(null);

  useEffect(() => {
    const cell = () => (Math.random() < density ? pick(glyphs) : " ");
    const g = Array.from({ length: cols * rows }, cell);
    const draw = () => {
      let s = "";
      for (let r = 0; r < rows; r++)
        s += g.slice(r * cols, (r + 1) * cols).join("") + "\n";
      if (ref.current) ref.current.textContent = s;
    };

    const step = () => {
      if (flow === "down") {
        g.copyWithin(cols, 0, cols * (rows - 1));
        for (let c = 0; c < cols; c++) g[c] = cell();
      } else if (flow === "up") {
        g.copyWithin(0, cols);
        for (let c = 0; c < cols; c++) g[cols * (rows - 1) + c] = cell();
      } else if (flow === "right") {
        for (let r = 0; r < rows; r++) {
          g.copyWithin(r * cols + 1, r * cols, (r + 1) * cols - 1);
          g[r * cols] = cell();
        }
      } else if (flow === "left") {
        for (let r = 0; r < rows; r++) {
          g.copyWithin(r * cols, r * cols + 1, (r + 1) * cols);
          g[(r + 1) * cols - 1] = cell();
        }
      } else {
        for (let k = 0; k < cols * rows * 0.12; k++)
          g[Math.floor(Math.random() * g.length)] = cell();
      }
    };

    draw();
    let f = 0;
    const t = setInterval(() => {
      step();
      draw();
      if (!forever && ++f > 140) clearInterval(t);
    }, ms);
    return () => clearInterval(t);
  }, []);

  return <pre ref={ref} className={className} aria-hidden="true" />;
};
