import { useEffect, useState } from "preact/hooks";
import style from "./Hud.module.scss";

const rnd = Math.random;

const REDS = ["#7a0610", "#a50e1a", "#c4182a"];

const makeSplat = () => {
  const u = 5 + Math.floor(rnd() * 5);
  const R = 2 + Math.floor(rnd() * 3);
  const cells: string[] = [];
  for (let dy = -R - 3; dy <= R + 3; dy++)
    for (let dx = -R - 3; dx <= R + 3; dx++) {
      const d = Math.hypot(dx, dy);
      const keep =
        d <= R ? rnd() < 1.1 - d / (R + 1) : d <= R + 3 && rnd() < 0.05;
      if (keep)
        cells.push(`${dx * u}px ${dy * u}px 0 ${REDS[Math.floor(rnd() * 3)]}`);
    }
  return {
    u,
    x: 3 + rnd() * 94,
    y: 3 + rnd() * 94,
    delay: rnd() * 0.5,
    len: 70 + Math.floor(rnd() * 150),
    t: 9 + rnd() * 9,
    shadow: cells.join(","),
  };
};

type Slot = {
  k: number;
  on: boolean;
  out: number;
  s: ReturnType<typeof makeSplat>;
};

const Blood = ({ count }: { count: number }) => {
  const [slots, setSlots] = useState<Slot[]>(() =>
    Array.from({ length: 14 }, () => ({
      k: 0,
      on: false,
      out: 0,
      s: makeSplat(),
    })),
  );

  useEffect(() => {
    setSlots((prev) =>
      prev.map((slot, i) => {
        const want = i < count;
        if (want && !slot.on)
          return { k: slot.k + 1, on: true, out: 0, s: makeSplat() };
        if (!want && slot.on) return { ...slot, on: false, out: rnd() * 2.6 };
        return slot;
      }),
    );
  }, [count]);

  return (
    <div className={style.blood2} aria-hidden="true">
      {slots.map(
        ({ k, on, out, s }, i) =>
          k > 0 && (
            <div
              key={i + "-" + k}
              className={on ? "" : style.out}
              style={{
                left: `${s.x}%`,
                top: `${s.y}%`,
                ["--od" as string]: `${out}s`,
              }}
            >
              <i
                style={{
                  width: s.u,
                  height: s.u,
                  animationDelay: `${s.delay}s`,
                  boxShadow: s.shadow,
                }}
              />
              <b
                style={{
                  width: s.u,
                  ["--len" as string]: `${s.len}px`,
                  ["--t" as string]: `${s.t}s`,
                  ["--d" as string]: `${s.delay + 0.8}s`,
                }}
              />
            </div>
          ),
      )}
    </div>
  );
};

export default Blood;
