package dev.zedkodex.modehud.client;

import java.util.HashMap;
import java.util.Map;

import dev.zedkodex.modehud.client.AsciiField.Flow;

public final class Places {
    private Places() {}

    public enum Realm { OVERWORLD, NETHER, END }

    public record Place(int id, Realm realm, String label, String name, String info, int color, String glyphs,
                        Flow flow, float density, int speed, String motion, int flash) {}

    public static final int JOURNEY = 1, PLAINS = 2, FOREST = 3, SNOWY = 4, SWAMP = 5, DESERT = 6, JUNGLE = 7,
            OCEAN = 8, MOUNTAINS = 9, CAVE = 10, DEEP_DARK = 11, NETHER = 12, WASTES = 13, CRIMSON = 14,
            WARPED = 15, SOUL = 16, BASALT = 17, BACK = 18, END = 19, END_ISLAND = 20, DRAGON = 21,
            END_CITY = 22, END_HIGH = 23, END_SMALL = 24, NETHER_PORTAL = 25, END_PORTAL = 26;

    private static final Map<Integer, Place> BY_ID = new HashMap<>();

    private static void add(int id, Realm realm, String label, String name, String info, int color, String glyphs,
                            Flow flow, float density, int speed, String motion, int flash) {
        BY_ID.put(id, new Place(id, realm, label, name, info, color, glyphs, flow, density, speed, motion, flash));
    }

    public static Place get(int id) {
        return BY_ID.get(id);
    }

    private static final String OW = "ВЕРХНИЙ МИР · ВЫ ВОШЛИ В";
    private static final String NE = "НИЖНИЙ МИР · ВЫ ВОШЛИ В";
    private static final String EN = "КРАЙ · ВЫ ВОШЛИ В";

    static {
        add(JOURNEY, Realm.OVERWORLD, "НАЧАЛО ПУТЕШЕСТВИЯ", "ВЕРХНИЙ МИР",
                "Ваша история начинается здесь. Мир огромен, а ночь опасна: найдите укрытие до заката",
                0xFFE9A8, ".*+'", Flow.UP, 0.04f, 180, "pulse", 0);
        add(PLAINS, Realm.OVERWORLD, OW, "РАВНИНЫ",
                "Открытые луга и редкие деревни. Здесь спокойно, пока не стемнеет",
                0x9BE564, ",.';\"", Flow.LEFT, 0.06f, 110, "sway", 0);
        add(FOREST, Realm.OVERWORLD, OW, "ЛЕС",
                "Густые кроны скрывают волков, грибные поляны и тропы, по которым легко заблудиться",
                0x4FBF4A, "&%*'", Flow.DOWN, 0.05f, 220, "sway", 0);
        add(SNOWY, Realm.OVERWORLD, OW, "СНЕЖНАЯ ТУНДРА",
                "Холодный ветер и бескрайний снег. Берегите тепло: вода здесь замерзает",
                0xCFEAFF, "*.+o", Flow.DOWN, 0.07f, 170, "none", 0);
        add(SWAMP, Realm.OVERWORLD, OW, "БОЛОТО",
                "Мутная вода, лианы и хижины ведьм. Под ногами хлюпает, а в воздухе висит туман",
                0x8AA84A, "~,;o", Flow.UP, 0.06f, 200, "sway", 0);
        add(DESERT, Realm.OVERWORLD, OW, "ПУСТЫНЯ",
                "Раскалённый песок, кактусы и забытые храмы. Воды хватит не на всех",
                0xFFD27A, ".:~-", Flow.RIGHT, 0.07f, 90, "none", 0);
        add(JUNGLE, Realm.OVERWORLD, OW, "ДЖУНГЛИ",
                "Тропические заросли, ручьи и храмы среди лиан. Здесь шумят попугаи и скрываются оцелоты",
                0x3FE07A, "|/\\^Y", Flow.UP, 0.06f, 160, "sway", 0);
        add(OCEAN, Realm.OVERWORLD, OW, "ОКЕАН",
                "Бескрайняя вода, подводные руины и кораллы. Не забывайте дышать",
                0x4DB8FF, "~=-^", Flow.RIGHT, 0.14f, 100, "sway", 0);
        add(MOUNTAINS, Realm.OVERWORLD, OW, "ГОРЫ",
                "Острые пики, обрывы и холодный воздух. Одно неверное движение и падение неизбежно",
                0xC9D3E0, "^A/\\#", Flow.NONE, 0.3f, 140, "none", 0);
        add(CAVE, Realm.OVERWORLD, "ВЕРХНИЙ МИР · ВЫ СПУСТИЛИСЬ", "ПОД ЗЕМЛЁЙ",
                "Темнота, эхо капель и тусклый свет факелов. Руда ждёт тех, кто копает глубже",
                0xC9B48A, ".:o'", Flow.DOWN, 0.03f, 260, "pulse", 0);
        add(DEEP_DARK, Realm.OVERWORLD, "ВЕРХНИЙ МИР · ВЫ СПУСТИЛИСЬ В", "ГЛУБОКУЮ ТЬМУ",
                "Здесь царит тишина. Не шумите: Хранитель слышит каждый ваш шаг",
                0x3FD6C6, ".o0*", Flow.NONE, 0.12f, 300, "flicker", 0);
        add(NETHER, Realm.NETHER, "ВЫ ВОШЛИ В", "НИЖНИЙ МИР",
                "Вы вошли в Ад. Здесь жарко, лава течёт рекой, а вода мгновенно испаряется",
                0xFF6A2A, "^*'.", Flow.UP, 0.09f, 110, "flicker", 0x9B30FF);
        add(WASTES, Realm.NETHER, NE, "НЕЗЕРСКИЕ ПУСТОШИ",
                "Раскалённые скалы, лавовые озёра и гасты, парящие в багровом небе",
                0xFF5A2A, "^'*.", Flow.UP, 0.08f, 120, "flicker", 0);
        add(CRIMSON, Realm.NETHER, NE, "БАГРОВЫЙ ЛЕС",
                "Алые грибы-деревья, хоглины и свиночерви. В воздухе плывут красные споры",
                0xFF3A4E, "|!'.", Flow.UP, 0.06f, 170, "sway", 0);
        add(WARPED, Realm.NETHER, NE, "ИСКАЖЁННЫЙ ЛЕС",
                "Бирюзовые грибы и тишина. Здесь бродят лишь эндермены",
                0x27E0C0, "|!.o", Flow.UP, 0.05f, 190, "sway", 0);
        add(SOUL, Realm.NETHER, NE, "ДОЛИНА ДУШ",
                "Песок душ, кости и синий огонь. Души шепчут в тумане",
                0x6FB4FF, ":;.~", Flow.UP, 0.06f, 200, "pulse", 0);
        add(BASALT, Realm.NETHER, NE, "БАЗАЛЬТОВЫЕ ДЕЛЬТЫ",
                "Чёрные колонны, пепел и магма. Каждый шаг может стать последним",
                0xB0B0B8, "#=%.", Flow.DOWN, 0.08f, 130, "none", 0);
        add(BACK, Realm.OVERWORLD, "ВЫ ВЕРНУЛИСЬ В", "ВЕРХНИЙ МИР",
                "Свежий воздух и солнечный свет. Портал остался позади",
                0xFFE9A8, "*.+'", Flow.UP, 0.05f, 160, "pulse", 0xFFFFFF);
        add(NETHER_PORTAL, Realm.NETHER, "ПОРТАЛ АКТИВИРОВАН", "ПОРТАЛ В АД",
                "Врата открыты, фиолетовое сияние зовёт в Нижний мир. Возьмите запас еды и воды",
                0xB44BFF, "^*o.", Flow.UP, 0.1f, 120, "flicker", 0x9B30FF);
        add(END_PORTAL, Realm.END, "ПОРТАЛ АКТИВИРОВАН", "ПОРТАЛ В КРАЙ",
                "Глаза Края замкнули кольцо. За порталом ждут остров, дракон и пустота",
                0xD9B3FF, "o.*+", Flow.NONE, 0.12f, 200, "pulse", 0x7A3CFF);
        add(END, Realm.END, "ВЫ ВОШЛИ В", "КРАЙ",
                "Вы шагнули в пустоту между мирами. Под ногами остров, вокруг лишь бездна и эндермены",
                0xD9B3FF, "o.*+", Flow.NONE, 0.1f, 220, "pulse", 0x7A3CFF);
        add(END_ISLAND, Realm.END, EN, "ЦЕНТРАЛЬНЫЙ ОСТРОВ",
                "Обсидиановые башни и кристаллы Края. Над островом кружит Эндер-дракон",
                0xE6C8FF, "o*.+", Flow.NONE, 0.13f, 180, "flicker", 0);
        add(DRAGON, Realm.END, "КРАЙ · ВЫ СРАЖАЕТЕСЬ С", "ЭНДЕР-ДРАКОНОМ",
                "Разрушьте кристаллы на башнях, иначе дракон будет исцеляться. Берегитесь его дыхания",
                0xC060FF, "~^<>=", Flow.LEFT, 0.1f, 70, "flicker", 0xA020F0);
        add(END_CITY, Realm.END, EN, "ГОРОД КРАЯ",
                "Пурпурные башни, шалкеры и корабль на горизонте. Внутри ждёт ценная добыча",
                0xE8A0FF, "#=|.", Flow.UP, 0.06f, 190, "none", 0);
        add(END_HIGH, Realm.END, EN, "ВЫСОКИЕ ЗЕМЛИ КРАЯ",
                "Далёкие острова с кораблями и городами. Здесь можно найти элитры",
                0xF0E6A0, ".:o*", Flow.RIGHT, 0.06f, 140, "sway", 0);
        add(END_SMALL, Realm.END, EN, "МАЛЫЕ ОСТРОВА КРАЯ",
                "Парящие осколки земли над бездной. Один шаг мимо, и вы падаете в пустоту",
                0xC9A8FF, "o.'*", Flow.DOWN, 0.05f, 230, "sway", 0);
    }
}
