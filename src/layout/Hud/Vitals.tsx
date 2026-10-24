import style from "./Hud.module.scss";

type Fx = { kind: "heal" | "hit"; n: number } | null;

const Cells = ({ value, small }: { value: number; small?: boolean }) => (
  <div className={`${style.cells} ${small ? style.small : ""}`}>
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
  bleeding: boolean;
  bandaging: boolean;
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
  bleeding,
  bandaging,
  fx,
}: Props) => (
  <div className={style.vitals}>
    {fx && <div key={fx.n} className={`${style.fx} ${style[fx.kind]}`} />}

    <div
      className={`${style.row} ${style.hp} ${hp <= 6 ? style.low : ""} ${bleeding ? style.bleeding : ""}`}
    >
      <div className={style.badge}>
        <img src="/1.png" alt="" />
        {bleeding && (
          <>
            <i className={style.blood} />
            <i
              className={style.blood}
              style={{ animationDelay: "0.6s", left: "62%" }}
            />
          </>
        )}
      </div>
      <Cells value={hp} />
      <span className={style.num}>{Math.ceil(hp)}</span>
    </div>

    <div className={`${style.row} ${style.xp}`}>
      <div key={lvlUp} className={`${style.lvl} ${lvlUp ? style.up : ""}`}>
        <small>LV</small>
        {lvl}
      </div>
      <div className={style.xpbar}>
        <div className={style.ghost} style={{ width: `${(xp / need) * 100}%` }} />
        <div className={style.fill} style={{ width: `${(xp / need) * 100}%` }} />
      </div>
      <span className={style.num}>
        {xp}/{need}
      </span>
    </div>

    {/* collapses to 0 height; food + water slide up/down with it */}
    <div className={`${style.status} ${bleeding || bandaging ? style.open : ""}`}>
      <div>
        {bleeding && !bandaging ? (
          <span key="bleed" className={`${style.chip} ${style.bleed}`}>
            <i /> КРОВОТЕЧЕНИЕ
          </span>
        ) : (
          <span key="wrap" className={`${style.chip} ${style.wrap}`}>
            ПЕРЕВЯЗКА
            <b className={style.wrapBar} />
          </span>
        )}
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

export default Vitals;
