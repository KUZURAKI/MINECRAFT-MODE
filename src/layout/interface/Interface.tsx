import style from "./Interface.module.scss";

const Interface = () => {
  return (
    <section>
      <div className={style.container}>
        <div className={style.containerUp}>
          <div class={style.itemsLeft}>
            <div>
              <p>LVL: 3</p>
              <p>EXP: 100</p>
            </div>
            <div>
              <div class={style.block}></div>
              <div class={style.block}></div>
              <div class={style.block}></div>
              <div class={style.block}></div>
            </div>
          </div>
          <div class={style.itemsRight}>
            <div>
              <div class={style.block}></div>
              <div class={style.block}></div>
              <div class={style.block}></div>
              <div class={style.block}></div>
            </div>
            <div>
              <div>
                <p>{"->"}</p>
              </div>
            </div>
            <div>
              <div class={style.block}></div>
            </div>
          </div>
        </div>
        <div className={style.containerCenter}>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
        </div>
        <div className={style.containerDown}>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
          <div class={style.block}></div>
        </div>
      </div>
    </section>
  );
};

export default Interface;
