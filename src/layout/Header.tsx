import style from "./Header.module.scss";

const Header = () => {
  return (
    <section className={style.header}>
      <div className={`container ${style.container}`}>
        <h1>ЗИМНИЙ БИОМ</h1>
        <p>Ваш лучший выбор для вашего биома</p>
      </div>
    </section>
  );
};

export default Header;
