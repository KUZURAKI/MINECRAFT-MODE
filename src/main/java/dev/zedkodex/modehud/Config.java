package dev.zedkodex.modehud;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public static final Common COMMON;
    public static final ModConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ModConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ModConfigSpec> c = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = c.getLeft();
        COMMON_SPEC = c.getRight();
        Pair<Client, ModConfigSpec> k = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = k.getLeft();
        CLIENT_SPEC = k.getRight();
    }

    public static class Common {
        public final ModConfigSpec.BooleanValue thirstEnabled;
        public final ModConfigSpec.DoubleValue waterSecondsPerPoint;
        public final ModConfigSpec.DoubleValue sprintMultiplier;
        public final ModConfigSpec.DoubleValue hotMultiplier;
        public final ModConfigSpec.IntValue thirstDamageTicks;
        public final ModConfigSpec.BooleanValue thirstSlowdown;

        public final ModConfigSpec.BooleanValue bleedEnabled;
        public final ModConfigSpec.DoubleValue bleedMinDamage;
        public final ModConfigSpec.DoubleValue bleedChance1;
        public final ModConfigSpec.DoubleValue bleedChance2;
        public final ModConfigSpec.DoubleValue bleedChance3;
        public final ModConfigSpec.DoubleValue bleedDamage2;
        public final ModConfigSpec.DoubleValue bleedDamage3;
        public final ModConfigSpec.IntValue bleedTicks1;
        public final ModConfigSpec.IntValue bleedTicks2;
        public final ModConfigSpec.IntValue bleedTicks3;
        public final ModConfigSpec.IntValue clotSeconds;

        public final ModConfigSpec.IntValue pillsHeal;
        public final ModConfigSpec.IntValue debtRegenTicks;

        Common(ModConfigSpec.Builder b) {
            b.push("thirst");
            thirstEnabled = b.comment("Enable the water stat").define("enabled", true);
            waterSecondsPerPoint = b.comment("Seconds of idle time to lose one water point (of 20)").defineInRange("secondsPerPoint", 90.0, 5.0, 3600.0);
            sprintMultiplier = b.comment("Water loss multiplier while sprinting").defineInRange("sprintMultiplier", 2.0, 1.0, 10.0);
            hotMultiplier = b.comment("Water loss multiplier in hot biomes and the Nether").defineInRange("hotMultiplier", 1.6, 1.0, 10.0);
            thirstDamageTicks = b.comment("Ticks between 1 hp damage at 0 water").defineInRange("damageTicks", 40, 5, 400);
            thirstSlowdown = b.comment("Slowness when water is 4 or lower").define("slowdown", true);
            b.pop();

            b.push("bleeding");
            bleedEnabled = b.comment("Enable bleeding").define("enabled", true);
            bleedMinDamage = b.comment("Minimum damage of one hit that can start light bleeding").defineInRange("minDamage", 2.0, 0.5, 20.0);
            bleedDamage2 = b.comment("Damage of one hit that can start medium bleeding").defineInRange("mediumDamage", 4.0, 1.0, 40.0);
            bleedDamage3 = b.comment("Damage of one hit that can start heavy bleeding").defineInRange("heavyDamage", 7.0, 1.0, 40.0);
            bleedChance1 = b.comment("Chance of light bleeding").defineInRange("chanceLight", 0.35, 0.0, 1.0);
            bleedChance2 = b.comment("Chance of medium bleeding").defineInRange("chanceMedium", 0.65, 0.0, 1.0);
            bleedChance3 = b.comment("Chance of heavy bleeding").defineInRange("chanceHeavy", 1.0, 0.0, 1.0);
            bleedTicks1 = b.comment("Ticks between 1 hp lost, light stage").defineInRange("ticksLight", 80, 4, 2000);
            bleedTicks2 = b.comment("Ticks between 1 hp lost, medium stage").defineInRange("ticksMedium", 40, 4, 2000);
            bleedTicks3 = b.comment("Ticks between 1 hp lost, heavy stage").defineInRange("ticksHeavy", 16, 4, 2000);
            clotSeconds = b.comment("Light bleeding stops by itself after this many seconds, 0 to disable").defineInRange("clotSeconds", 45, 0, 3600);
            b.pop();

            b.push("healing");
            pillsHeal = b.comment("Hp restored by painkillers").defineInRange("pillsHeal", 6, 1, 20);
            debtRegenTicks = b.comment("Ticks between 1 hp restored after hunger, thirst or drowning damage once the cause is fixed").defineInRange("debtRegenTicks", 50, 5, 600);
            b.pop();
        }
    }

    public static class Client {
        public final ModConfigSpec.BooleanValue hudEnabled;
        public final ModConfigSpec.BooleanValue eyesIntro;
        public final ModConfigSpec.BooleanValue notifications;
        public final ModConfigSpec.BooleanValue bloodEffects;
        public final ModConfigSpec.BooleanValue asciiEffects;
        public final ModConfigSpec.DoubleValue hudScale;
        public final ModConfigSpec.DoubleValue toastScale;

        Client(ModConfigSpec.Builder b) {
            hudEnabled = b.comment("Replace the vanilla hud").define("hudEnabled", true);
            eyesIntro = b.comment("Eye opening effect after joining a world").define("eyesIntro", true);
            notifications = b.comment("Biome and realm notifications").define("notifications", true);
            bloodEffects = b.comment("Blood splatters and red screen edges at low hp").define("bloodEffects", true);
            asciiEffects = b.comment("Symbol effects (ascii) on text and backgrounds").define("asciiEffects", true);
            hudScale = b.comment("Size of the vitals panel and hotbar").defineInRange("hudScale", 0.6, 0.25, 3.0);
            toastScale = b.comment("Size of biome and realm notifications").defineInRange("toastScale", 0.75, 0.25, 3.0);
        }
    }
}
