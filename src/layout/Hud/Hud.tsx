import { useEffect, useState } from "preact/hooks";
import style from "./Hud.module.scss";
import { slots } from "./Hud.tupe";

const clamp = (n: number) => Math.min(Math.max(n, 0), 20);

const Bar = ({
  value,
  kind,
  icon,
}: {
  value: number;
  kind: "hp" | "food";
  icon: string;
}) => (
  <div className={`${style.bar} ${style[kind]} ${value <= 6 ? style.low : ""}`}>
    <div className={style.badge}>
      <img src={icon} alt="" />
    </div>
    <div className={style.cells}>
      {Array.from({ length: 10 }, (_, i) => {
        const w = `${Math.min(Math.max((value - i * 2) / 2, 0), 1) * 100}%`;
        return (
          <div key={i} className={style.cell}>
            <div className={style.ghost} style={{ width: w }} />
            <div className={style.fill} style={{ width: w }} />
          </div>
        );
      })}
    </div>
    <span className={style.num}>{value}</span>
  </div>
);

const Hud = () => {
  const [active, setActive] = useState(0);
  const [hp, setHp] = useState(13);
  const [food, setFood] = useState(16);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const n = Number(e.key);
      if (n >= 1 && n <= slots.length) return setActive(n - 1);
      if (e.key === "ArrowDown") setHp((v) => clamp(v - 3));
      if (e.key === "ArrowUp") setHp((v) => clamp(v + 3));
      if (e.key === "ArrowLeft") setFood((v) => clamp(v - 3));
      if (e.key === "ArrowRight") setFood((v) => clamp(v + 3));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <>
      {hp <= 6 && <div className={style.vignette} />}
      <aside className={style.hud}>
        <div className={style.vitals}>
          <Bar value={hp} kind="hp" icon="/1.png" />
          <Bar value={food} kind="food" icon="/2.png" />
        </div>

        <div className={style.slots}>
          {slots.map((s, i) => (
            <button
              key={s.name}
              className={`${style.slot} ${i === active ? style.active : ""}`}
              onClick={() => setActive(i)}
            >
              <span className={style.gem}>
                <span className={style.icon}>{s.icon}</span>
              </span>
              <span className={style.key}>{i + 1}</span>
              {s.count && <span className={style.count}>×{s.count}</span>}
              {s.durability !== undefined && (
                <span className={style.dur}>
                  <span
                    style={{
                      width: `${s.durability * 100}%`,
                      ["--d" as string]: s.durability,
                    }}
                  />
                </span>
              )}
            </button>
          ))}
        </div>
      </aside>
    </>
  );
};

export default Hud;
