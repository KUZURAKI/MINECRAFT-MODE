import { useState } from "preact/hooks";
import style from "./Eyes.module.scss";
import { AsciiBg } from "../Ascii";

const Eyes = () => {
  const [done, setDone] = useState(false);
  if (done) return null;

  return (
    <div className={style.eyes}>
      <div className={style.haze} />
      <AsciiBg
        className={style.ascii}
        cols={Math.ceil(window.innerWidth / 12)}
        rows={Math.ceil(window.innerHeight / 16)}
        ms={80}
      />
      <div className={style.dark} onAnimationEnd={() => setDone(true)} />
    </div>
  );
};

export default Eyes;
