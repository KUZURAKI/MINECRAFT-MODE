import { useRef } from "preact/hooks";
import style from "./Hud.module.scss";
import { AsciiBg, Scramble } from "../Ascii";

type Fx = { kind: "heal" | "hit" | "hunger" | "thirst"; n: number } | null;

const Cells = ({
  value,
  small,
  hurt,
}: {
  value: number;
  small?: boolean;
  hurt?: boolean;
}) => (
  <div
    className={`${style.cells} ${small ? style.small : ""} ${hurt ? style.hurt : ""}`}
  >
    {Array.from({ length: 10 }, (_, i) => {
      const pts = Math.min(Math.max(Math.floor(value) - i * 2, 0), 2);
      return (
        <div key={i} className={style.cell}>
          <div className={style.fill} style={{ width: `${pts * 50}%` }} />
          <span className={style.glyph}>
            <Scramble
              start={i}
              text={pts === 2 ? "#" : pts === 1 ? "=" : "."}
              rate={0.5}
            />
          </span>
        </div>
      );
    })}
  </div>
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
  air: number;
  airShown: boolean;
  fx: Fx;
};

const Vitals = ({
  hp,
  food,
  water,
  // lvl,
  // xp,
  // need,
  // lvlUp,
  bleed,
  air,
  airShown,
  fx,
}: Props) => {
  const bleeding = bleed > 0;
  const last = useRef(1);
  if (bleed) last.current = bleed;
  const stage = last.current;

  return (
    <div className={style.vitals}>
      <AsciiBg className={style.hudBg} cols={44} rows={13} ms={140} forever />
      {fx && <div key={fx.n} className={`${style.fx} ${style[fx.kind]}`} />}

      {/*
      <div className={`${style.row} ${style.xp}`}>
        <div key={lvlUp} className={`${style.lvl} ${lvlUp ? style.up : ""}`}>
          <img src="/lvl.png" alt="" />
          <span>
            <Scramble text={String(lvl)} rate={0.3} />
          </span>
        </div>
        <Cells value={(xp / need) * 20} />
      </div>
      */}

      <div
        className={`${style.row} ${style.hp} ${hp <= 6 ? style.low : ""} ${hp <= 3 ? style.critical : ""} ${bleeding ? style.bleeding : ""}`}
      >
        <div className={style.badge}>
          <img src="/hp.png" alt="" />
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
      </div>

      <div className={`${style.status} ${bleeding ? style.open : ""}`}>
        <div>
          <div className={style.pad}>
            <span className={`${style.chip} ${style[`s${stage}`]}`}>
              <span className={style.ico}>
                <img src={"/blod-" + stage + ".png"} alt="" />
                <b>
                  <Scramble key={bleed} text={String(stage)} rate={0.4} />
                </b>
              </span>
              <Scramble key={bleed} text="КРОВОТЕЧЕНИЕ" rate={0.6} />
            </span>
          </div>
        </div>
      </div>

      <div className={style.needs}>
        <div
          className={`${style.row} ${style.need} ${style.food} ${food <= 4 ? style.low : ""}`}
        >
          <div className={style.mini}>
            <img src="/eat.png" alt="" />
          </div>
          <Cells
            key={fx?.kind === "hunger" ? fx.n : "c"}
            value={food}
            small
            hurt={fx?.kind === "hunger"}
          />
        </div>
        <div
          className={`${style.row} ${style.need} ${style.water} ${water <= 4 ? style.low : ""}`}
        >
          <div className={style.mini}>
            <img src="/vater.png" alt="" />
          </div>
          <Cells
            key={fx?.kind === "thirst" ? fx.n : "c"}
            value={water}
            small
            hurt={fx?.kind === "thirst"}
          />
        </div>
      </div>

      <div className={`${style.air} ${airShown ? style.open : ""}`}>
        <div>
          <div className={style.airPad}>
            <div
              className={`${style.row} ${style.need} ${style.oxy} ${air <= 4 ? style.low : ""}`}
            >
              <div className={style.mini}>
                <img src="/kis.png" alt="" />
              </div>
              <Cells value={air} small />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Vitals;
