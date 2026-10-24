import { useRef } from "preact/hooks";
import style from "./Hud.module.scss";

type Fx = { kind: "heal" | "hit"; n: number } | null;

// each cell is 2 points and snaps: empty -> half -> full (no smooth fill)
const Cells = ({ value, small }: { value: number; small?: boolean }) => (
  <div className={`${style.cells} ${small ? style.small : ""}`}>
    {Array.from({ length: 10 }, (_, i) => {
      const pts = Math.min(Math.max(Math.floor(value) - i * 2, 0), 2);
      return (
        <div key={i} className={style.cell}>
          <div className={style.fill} style={{ width: `${pts * 50}%` }} />
        </div>
      );
    })}
  </div>
);

const Drop = () => (
  <svg viewBox="0 0 9 12" shape-rendering="crispEdges">
    <path
      d="M4 0h1v2h1v2h1v2h1v4h-1v1h-1v1h-3v-1h-1v-1h-1v-4h1v-2h1v-2h1z"
      fill="#4db8ff"
    />
    <path d="M2 7h1v2h-1z" fill="#fff" opacity=".6" />
  </svg>
);

type Props = {
  hp: number;
  food: number;
  water: number;
  lvl: number;
  xp: number;
  need: number;
  lvlUp: number;
  bleed: number;
  fx: Fx;
};

const Vitals = ({
  hp,
  food,
  water,
  lvl,
  xp,
  need,
  lvlUp,
  bleed,
  fx,
}: Props) => {
  const bleeding = bleed > 0;
  // keep the last stage so the chip doesn't change text while it collapses
  const last = useRef(1);
  if (bleed) last.current = bleed;
  const stage = last.current;

  return (
    <div className={style.vitals}>
      {fx && <div key={fx.n} className={`${style.fx} ${style[fx.kind]}`} />}

      <div className={`${style.row} ${style.xp}`}>
        <div key={lvlUp} className={`${style.lvl} ${lvlUp ? style.up : ""}`}>
          <small>LV</small>
          {lvl}
        </div>
        <div className={style.xpbar}>
          <div
            className={style.fill}
            style={{ width: `${(xp / need) * 100}%` }}
          />
        </div>
        <span className={style.num}>
          {xp}/{need}
        </span>
      </div>

      <div
        className={`${style.row} ${style.hp} ${hp <= 6 ? style.low : ""} ${hp <= 3 ? style.critical : ""} ${bleeding ? style.bleeding : ""}`}
      >
        <div className={style.badge}>
          <img src="/1.png" alt="" />
          {bleeding &&
            Array.from({ length: bleed }, (_, i) => (
              <i
                key={i}
                className={style.blood}
                style={{
                  left: `${34 + i * 16}%`,
                  animationDelay: `${i * 0.25}s`,
                  animationDuration: `${[0, 2, 1.2, 0.7][bleed]}s`,
                }}
              />
            ))}
        </div>
        <Cells value={hp} />
        <span className={style.num}>{Math.ceil(hp)}</span>
      </div>

      {/* collapses to 0 height; food + water slide up/down with it */}
      <div className={`${style.status} ${bleeding ? style.open : ""}`}>
        <div>
          <div className={style.pad}>
            <span
              className={`${style.chip} ${style.bleed} ${style[`s${stage}`]}`}
            >
              КРОВОТЕЧЕНИЕ
              <em>
                {[1, 2, 3].map((n) => (
                  <i key={n} className={n <= stage ? style.on : ""} />
                ))}
              </em>
            </span>
          </div>
        </div>
      </div>

      <div className={style.needs}>
        <div className={`${style.row} ${style.need} ${style.food}`}>
          <div className={style.mini}>
            <img src="/2.png" alt="" />
          </div>
          <Cells value={food} small />
        </div>
        <div className={`${style.row} ${style.need} ${style.water}`}>
          <div className={style.mini}>
            <Drop />
          </div>
          <Cells value={water} small />
        </div>
      </div>
    </div>
  );
};

export default Vitals;
