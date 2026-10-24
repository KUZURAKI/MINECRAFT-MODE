import { useEffect, useRef, useState } from "preact/hooks";
import style from "./Hud.module.scss";
import { slots } from "./Hud.tupe";
import Vitals from "./Vitals";
import Hotbar from "./Hotbar";
import Blood from "./Blood";

const MAX = 20;
// walks a displayed value toward its target one point at a time (heal and damage both animate)
const useStep = (target: number, ms = 110) => {
  const [v, setV] = useState(target);
  useEffect(() => {
    if (v === target) return;
    const t = setTimeout(() => setV((x) => x + Math.sign(target - x)), ms);
    return () => clearTimeout(t);
  }, [v, target]);
  return v;
};

// bleed stage 1..3 -> ms between 1-point losses
const BLEED_MS = [0, 4000, 2000, 800];

const clamp = (n: number) => Math.min(Math.max(n, 0), MAX);

const Hud = () => {
  const [items, setItems] = useState(slots);
  const [active, setActive] = useState(0);
  const [hp, setHp] = useState(13);
  const [food, setFood] = useState(16);
  const [water, setWater] = useState(12);
  const [lvl, setLvl] = useState(3);
  const [xp, setXp] = useState(20);
  const [lvlUp, setLvlUp] = useState(0);
  const [bleed, setBleed] = useState(0);
  const [fx, setFx] = useState<{ kind: "heal" | "hit"; n: number } | null>(
    null,
  );

  const [barShown, setBarShown] = useState(true);
  const hideTimer = useRef(0);

  const flash = (kind: "heal" | "hit") =>
    setFx((f) => ({ kind, n: (f?.n ?? 0) + 1 }));

  const reveal = () => {
    setBarShown(true);
    clearTimeout(hideTimer.current);
    hideTimer.current = window.setTimeout(() => setBarShown(false), 2000);
  };

  useEffect(() => {
    reveal();
    return () => clearTimeout(hideTimer.current);
  }, []);

  const shownHp = useStep(hp);
  const shownFood = useStep(food);
  const shownWater = useStep(water);

  const need = 40 + lvl * 20;

  const gainXp = (n: number) => {
    let l = lvl;
    let x = xp + n;
    while (x >= 40 + l * 20) {
      x -= 40 + l * 20;
      l++;
    }
    setXp(x);
    if (l > lvl) {
      setLvl(l);
      setLvlUp((c) => c + 1);
      setHp((v) => clamp(v + 2));
      flash("heal");
    }
  };

  const select = (i: number) => {
    setActive(i);
    reveal();
  };

  useEffect(() => {
    if (!bleed) return;
    const t = setInterval(() => setHp((v) => clamp(v - 1)), BLEED_MS[bleed]);
    return () => clearInterval(t);
  }, [bleed]);

  const use = () => {
    reveal();
    const s = items[active];
    if (!s.count) return;
    const spend = () =>
      setItems((l) =>
        l.map((x, i) => (i === active ? { ...x, count: x.count! - 1 } : x)),
      );

    if (s.kind === "pills" && hp < MAX) {
      spend();
      setHp((v) => clamp(v + 6));
      flash("heal");
    } else if (s.kind === "bandage" && bleed) {
      spend();
      setBleed(0);
      flash("heal");
    } else if (s.kind === "food" && food < MAX) {
      spend();
      setFood((v) => clamp(v + 6));
    } else if (s.kind === "water" && water < MAX) {
      spend();
      setWater((v) => clamp(v + 8));
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const n = Number(e.key);
      if (n >= 1 && n <= items.length) return select(n - 1);
      const k = e.key.toLowerCase();
      if (k === "f") use();
      if (k === "k") setBleed((b) => (b + 1) % 4);
      if (k === "x") gainXp(25);
      if (e.key === "ArrowDown") {
        setHp((v) => clamp(v - 3));
        flash("hit");
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  return (
    <>
      {hp <= 6 && (
        <div className={`${style.vignette} ${hp <= 3 ? style.critical : ""}`} />
      )}
      {hp <= 6 && <Blood count={hp <= 3 ? 14 : 5} />}
      <aside className={style.hud}>
        <Vitals
          hp={shownHp}
          food={shownFood}
          water={shownWater}
          lvl={lvl}
          xp={xp}
          need={need}
          lvlUp={lvlUp}
          bleed={bleed}
          fx={fx}
        />
      </aside>
      <Hotbar
        items={items}
        active={active}
        hidden={!barShown}
        onSelect={select}
      />
      <p className={style.hint}>
        1-7 слот · F применить · ↓ урон · K кровотечение · X опыт · B биом
      </p>
    </>
  );
};

export default Hud;
