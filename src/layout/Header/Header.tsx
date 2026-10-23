import style from "./Header.module.scss";
import { biom } from "./Header.type";

const Header = () => {
  return (
    <section className={style.header}>
      <div className={`container ${style.container}`}>
        <h2>{biom[0].name}</h2>
        <p>{biom[0].info}</p>
      </div>
    </section>
  );
};

export default Header;
