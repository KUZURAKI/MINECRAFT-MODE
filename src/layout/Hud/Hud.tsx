import { useEffect, useRef, useState } from "preact/hooks";
import style from "./Hud.module.scss";
import { slots } from "./Hud.tupe";
import Vitals from "./Vitals";
import Hotbar from "./Hotbar";

const MAX = 20;
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
  const [bleeding, setBleeding] = useState(false);
  const [bandaging, setBandaging] = useState(false);
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
    if (!bleeding) return;
    const t = setInterval(() => setHp((v) => clamp(v - 0.5)), 1000);
    return () => clearInterval(t);
  }, [bleeding]);

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
    } else if (s.kind === "bandage" && bleeding && !bandaging) {
      spend();
      setBandaging(true);
      setTimeout(() => {
        setBandaging(false);
        setBleeding(false);
        flash("heal");
      }, 1600);
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
      if (k === "k") setBleeding((b) => !b);
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
      {hp <= 6 && <div className={style.vignette} />}
      <aside className={style.hud}>
        <Vitals
          hp={hp}
          food={food}
          water={water}
          lvl={lvl}
          xp={xp}
          need={need}
          lvlUp={lvlUp}
          bleeding={bleeding}
          bandaging={bandaging}
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
