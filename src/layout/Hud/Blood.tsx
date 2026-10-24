import style from "./Hud.module.scss";

// seeded rng: same splatter every time, no layout jumps between renders
let seed = 7;
const rnd = () => {
  seed = (seed * 16807) % 2147483647;
  return seed / 2147483647;
};

const REDS = ["#7a0610", "#a50e1a", "#c4182a"];

// one pixel blob: dense core, ragged edge, a few stray droplets
const makeSplat = () => {
  const u = 5 + Math.floor(rnd() * 5);
  const R = 2 + Math.floor(rnd() * 3);
  const cells: string[] = [];
  for (let dy = -R - 3; dy <= R + 3; dy++)
    for (let dx = -R - 3; dx <= R + 3; dx++) {
      const d = Math.hypot(dx, dy);
      const keep = d <= R ? rnd() < 1.1 - d / (R + 1) : d <= R + 3 && rnd() < 0.05;
      if (keep) cells.push(`${dx * u}px ${dy * u}px 0 ${REDS[Math.floor(rnd() * 3)]}`);
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

const splats = Array.from({ length: 14 }, makeSplat);

// count: how many splats are visible; same first N each time so none replay
const Blood = ({ count }: { count: number }) => (
  <div className={style.blood2} aria-hidden="true">
    {splats.slice(0, count).map((s, i) => (
      <div key={i} style={{ left: `${s.x}%`, top: `${s.y}%` }}>
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
    ))}
  </div>
);

export default Blood;
