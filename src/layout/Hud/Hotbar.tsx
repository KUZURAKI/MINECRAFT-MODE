import style from "./Hud.module.scss";
import { Scramble } from "../Ascii";
import type { Slot } from "./Hud.tupe";

const Hotbar = ({
  items,
  active,
  hidden,
  onSelect,
}: {
  items: Slot[];
  active: number;
  hidden: boolean;
  onSelect: (i: number) => void;
}) => (
  <nav className={`${style.hotbar} ${hidden ? style.hidden : ""}`}>
    {items.map((s, i) => (
      <button
        key={s.name}
        aria-label={s.name}
        className={`${style.slot} ${i === active ? style.active : ""} ${s.count === 0 ? style.empty : ""}`}
        onClick={() => onSelect(i)}
      >
        <span className={style.icon}>{s.icon}</span>
        <span className={style.key}>
          <Scramble text={String(i + 1)} />
        </span>
        {i === active && <i className={style.dot} />}
        {!!s.count && (
          <span className={style.count}>
            <Scramble text={String(s.count)} rate={0.4} />
          </span>
        )}
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
  </nav>
);

export default Hotbar;
